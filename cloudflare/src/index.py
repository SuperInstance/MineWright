"""
Minecraft Agent Reflex - Cloudflare Worker
Main entry point for the reflex agent that serves tactical decisions
to Minecraft MineWright agents via Durable Objects.

This worker implements:
- AgentState Durable Object for per-agent state management
- Fast tactical decision endpoints (<50ms response time)
- Strategic mission queue integration
- Bi-directional sync with Foreman orchestrator
"""

from dataclasses import dataclass, field
from datetime import datetime, timedelta
from enum import Enum
from typing import Any, Optional
import json

from cloudflare_workers import (
    Env,
    Request,
    Response,
    DurableObject,
    DurableObjectState,
)


class AgentStatus(Enum):
    """Agent lifecycle states"""
    IDLE = "idle"
    PLANNING = "planning"
    EXECUTING = "executing"
    WAITING = "waiting"
    COMBAT = "combat"
    ERROR = "error"


@dataclass
class Position:
    """3D position in Minecraft world"""
    x: float
    y: float
    z: float

    def to_dict(self) -> dict:
        return {"x": self.x, "y": self.y, "z": self.z}

    @classmethod
    def from_dict(cls, data: dict) -> "Position":
        return cls(x=data["x"], y=data["y"], z=data["z"])


@dataclass
class Threat:
    """Combat threat assessment"""
    entity_type: str
    position: Position
    distance: float
    danger_level: float  # 0.0 to 1.0
    estimated_health: int

    def to_dict(self) -> dict:
        return {
            "entityType": self.entity_type,
            "position": self.position.to_dict(),
            "distance": self.distance,
            "dangerLevel": self.danger_level,
            "estimatedHealth": self.estimated_health,
        }


@dataclass
class Hazard:
    """Environmental hazard"""
    hazard_type: str  # "lava", "fall", "fire", "suffocation"
    position: Position
    severity: float  # 0.0 to 1.0

    def to_dict(self) -> dict:
        return {
            "hazardType": self.hazard_type,
            "position": self.position.to_dict(),
            "severity": self.severity,
        }


@dataclass
class TacticalDecision:
    """Quick tactical decision response"""
    action: str  # "flee", "attack", "shield", "dodge", "hold"
    priority: float  # 0.0 to 1.0
    reasoning: str
    target_position: Optional[Position] = None
    confidence: float = 1.0

    def to_dict(self) -> dict:
        result = {
            "action": self.action,
            "priority": self.priority,
            "reasoning": self.reasoning,
            "confidence": self.confidence,
        }
        if self.target_position:
            result["targetPosition"] = self.target_position.to_dict()
        return result


@dataclass
class AgentState:
    """Persisted agent state in Durable Object storage"""
    agent_id: str
    status: AgentStatus
    position: Position
    health: int
    hunger: int
    current_task: Optional[str] = None
    inventory_slots: int = 36
    last_active: datetime = field(default_factory=datetime.utcnow)
    active_threats: list[Threat] = field(default_factory=list)
    known_hazards: list[Hazard] = field(default_factory=list)
    mission_queue: list[dict] = field(default_factory=list)
    telemetry_events: list[dict] = field(default_factory=list)


class AgentStateDO(DurableObject):
    """
    Durable Object for managing individual agent state.

    Provides fast tactical decisions and maintains persistent state
    for each Minecraft Steve agent.
    """

    def __init__(self, state: DurableObjectState, env: Env):
        super().__init__(state, env)
        self.state = state
        self.env = env
        self.storage = state.storage
        self.agent_id: Optional[str] = None
        self.state_data: Optional[AgentState] = None

        # Initialize AI binding for reasoning
        self.ai = env.AI if hasattr(env, "AI") else None
        self.vectorize = env.VECTORIZE if hasattr(env, "VECTORIZE") else None

    async def fetch(self, request: Request) -> Response:
        """Main request handler"""
        url = request.url
        path = url.path

        try:
            # Route to appropriate handler
            if path.endswith("/sync"):
                return await self._handle_sync(request)
            elif path.endswith("/mission"):
                return await self._handle_mission(request)
            elif path.endswith("/tactical"):
                return await self._handle_tactical(request)
            elif path.endswith("/telemetry"):
                return await self._handle_telemetry(request)
            elif path.endswith("/health"):
                return await self._handle_health()
            else:
                return Response.json(
                    {"error": "Not found", "path": path},
                    status=404
                )

        except Exception as e:
            # Log error and return 500
            await self._log_error("fetch", str(e))
            return Response.json(
                {"error": "Internal server error", "message": str(e)},
                status=500
            )

    async def _ensure_loaded(self):
        """Ensure agent state is loaded from storage"""
        if self.state_data is None:
            data = await self.storage.get("state")
            if data:
                # Reconstruct AgentState from stored dict
                self.state_data = self._deserialize_state(data)
            else:
                # Initialize new state
                self.agent_id = self.state.id
                self.state_data = AgentState(
                    agent_id=self.agent_id,
                    status=AgentStatus.IDLE,
                    position=Position(x=0, y=64, z=0),
                    health=20,
                    hunger=20,
                )
                await self._save_state()

    async def _save_state(self):
        """Persist state to storage"""
        if self.state_data:
            await self.storage.put("state", self._serialize_state(self.state_data))
            self.state_data.last_active = datetime.utcnow()

    def _serialize_state(self, state: AgentState) -> dict:
        """Serialize state to dict for storage"""
        return {
            "agentId": state.agent_id,
            "status": state.status.value,
            "position": state.position.to_dict(),
            "health": state.health,
            "hunger": state.hunger,
            "currentTask": state.current_task,
            "inventorySlots": state.inventory_slots,
            "lastActive": state.last_active.isoformat(),
            "activeThreats": [t.to_dict() for t in state.active_threats],
            "knownHazards": [h.to_dict() for h in state.known_hazards],
            "missionQueue": state.mission_queue,
            "telemetryEvents": state.telemetry_events[-100:],  # Keep last 100
        }

    def _deserialize_state(self, data: dict) -> AgentState:
        """Deserialize dict to AgentState"""
        return AgentState(
            agent_id=data["agentId"],
            status=AgentStatus(data["status"]),
            position=Position.from_dict(data["position"]),
            health=data["health"],
            hunger=data["hunger"],
            current_task=data.get("currentTask"),
            inventory_slots=data.get("inventorySlots", 36),
            last_active=datetime.fromisoformat(data["lastActive"]),
            active_threats=[Threat(**t) for t in data.get("activeThreats", [])],
            known_hazards=[Hazard(**h) for h in data.get("knownHazards", [])],
            mission_queue=data.get("missionQueue", []),
            telemetry_events=data.get("telemetryEvents", []),
        )

    async def _handle_sync(self, request: Request) -> Response:
        """
        Handle sync requests from Foreman orchestrator.

        GET /sync - Get current state
        POST /sync - Update state from Foreman
        """
        await self._ensure_loaded()

        if request.method == "GET":
            # Return current state
            return Response.json(self._serialize_state(self.state_data))

        elif request.method == "POST":
            # Update state from Foreman
            body = await request.json()

            # Update fields from Foreman
            if "position" in body:
                self.state_data.position = Position.from_dict(body["position"])
            if "status" in body:
                self.state_data.status = AgentStatus(body["status"])
            if "health" in body:
                self.state_data.health = body["health"]
            if "hunger" in body:
                self.state_data.hunger = body["hunger"]
            if "currentTask" in body:
                self.state_data.current_task = body["currentTask"]

            # Add mission from Foreman if provided
            if "mission" in body:
                self.state_data.mission_queue.append(body["mission"])

            await self._save_state()

            return Response.json({
                "status": "synced",
                "agentId": self.state_data.agent_id,
                "timestamp": datetime.utcnow().isoformat(),
            })

        else:
            return Response.json({"error": "Method not allowed"}, status=405)

    async def _handle_mission(self, request: Request) -> Response:
        """
        Handle mission queue operations.

        GET /mission - Get current mission queue
        POST /mission - Add new mission
        DELETE /mission - Clear completed mission
        """
        await self._ensure_loaded()

        if request.method == "GET":
            return Response.json({
                "agentId": self.state_data.agent_id,
                "missions": self.state_data.mission_queue,
                "current": self.state_data.current_task,
            })

        elif request.method == "POST":
            body = await request.json()
            mission = body.get("mission")

            if not mission:
                return Response.json({"error": "Missing mission"}, status=400)

            self.state_data.mission_queue.append(mission)
            await self._save_state()

            return Response.json({
                "status": "queued",
                "queueLength": len(self.state_data.mission_queue),
            })

        elif request.method == "DELETE":
            # Remove completed mission from queue
            if self.state_data.mission_queue:
                completed = self.state_data.mission_queue.pop(0)
                await self._save_state()
                return Response.json({"completed": completed})
            else:
                return Response.json({"error": "No missions to complete"}, status=404)

        else:
            return Response.json({"error": "Method not allowed"}, status=405)

    async def _handle_tactical(self, request: Request) -> Response:
        """
        Handle tactical decision requests.

        POST /tactical - Get quick tactical decision based on current situation
        """
        await self._ensure_loaded()

        if request.method != "POST":
            return Response.json({"error": "Method not allowed"}, status=405)

        body = await request.json()

        # Update current position
        if "position" in body:
            self.state_data.position = Position.from_dict(body["position"])

        # Extract tactical context
        nearby_entities = body.get("nearbyEntities", [])
        nearby_blocks = body.get("nearbyBlocks", [])
        current_health = body.get("health", self.state_data.health)
        combat_score = body.get("combatScore", 0.0)

        # Import tactical analysis functions
        from .tactical import (
            assess_threats,
            detect_hazards,
            get_quick_decision,
            prioritize_resources,
        )

        # Assess threats
        threats = assess_threats(
            agent_position=self.state_data.position,
            nearby_entities=nearby_entities,
            current_health=current_health,
        )

        # Detect hazards
        hazards = detect_hazards(
            agent_position=self.state_data.position,
            nearby_blocks=nearby_blocks,
        )

        # Update state
        self.state_data.active_threats = threats
        self.state_data.known_hazards = hazards
        self.state_data.health = current_health

        # Get quick tactical decision
        decision = get_quick_decision(
            threats=threats,
            hazards=hazards,
            combat_score=combat_score,
            current_mission=self.state_data.current_task,
        )

        # Log telemetry
        await self._log_telemetry("tactical", {
            "decision": decision.to_dict(),
            "threats": len(threats),
            "hazards": len(hazards),
        })

        await self._save_state()

        return Response.json({
            "decision": decision.to_dict(),
            "threats": [t.to_dict() for t in threats],
            "hazards": [h.to_dict() for h in hazards],
            "timestamp": datetime.utcnow().isoformat(),
        })

    async def _handle_telemetry(self, request: Request) -> Response:
        """
        Handle telemetry logging.

        POST /telemetry - Log telemetry event
        """
        await self._ensure_loaded()

        if request.method != "POST":
            return Response.json({"error": "Method not allowed"}, status=405)

        body = await request.json()
        event_type = body.get("type", "unknown")
        event_data = body.get("data", {})

        await self._log_telemetry(event_type, event_data)

        return Response.json({"status": "logged"})

    async def _handle_health(self) -> Response:
        """Health check endpoint"""
        return Response.json({
            "status": "healthy",
            "agentId": self.state_data.agent_id if self.state_data else "uninitialized",
            "timestamp": datetime.utcnow().isoformat(),
        })

    async def _log_telemetry(self, event_type: str, data: dict):
        """Log telemetry event to state"""
        await self._ensure_loaded()

        event = {
            "type": event_type,
            "data": data,
            "timestamp": datetime.utcnow().isoformat(),
        }

        self.state_data.telemetry_events.append(event)

        # Keep only last 100 events
        if len(self.state_data.telemetry_events) > 100:
            self.state_data.telemetry_events = self.state_data.telemetry_events[-100:]

        await self._save_state()

    async def _log_error(self, context: str, message: str):
        """Log error to state and external logging"""
        await self._log_telemetry("error", {
            "context": context,
            "message": message,
        })


# Worker entry point
async def fetch(request: Request, env: Env) -> Response:
    """
    Main worker entry point.

    Routes requests to the appropriate Durable Object stub
    based on the agent_id in the URL.
    """
    url = request.url
    path = url.path

    # Extract agent_id from path: /agents/{agent_id}/...
    parts = path.strip("/").split("/")

    if len(parts) < 2 or parts[0] != "agents":
        return Response.json({
            "error": "Invalid path",
            "expected": "/agents/{agent_id}/endpoint",
        }, status=400)

    agent_id = parts[1]

    # Get Durable Object stub for this agent
    stub = env.AGENT_STATE.get(id=agent_id)

    # Forward request to the Durable Object
    return await stub.fetch(request)


# Scheduled handler for heartbeat to Foreman
async def scheduled(event: Any, env: Env, ctx: Any) -> None:
    """
    Scheduled task handler for heartbeat to Foreman orchestrator.

    Runs every 5 minutes to sync agent status with Foreman.
    """
    # This would trigger a sync to Foreman's centralized status
    # Implementation depends on Foreman's sync protocol
    pass
