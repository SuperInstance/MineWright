"""
Sync Module for Minecraft Agent Reflex

Handles bi-directional synchronization with Foreman orchestrator.
Manages state sync, telemetry logging, and mission retrieval.
"""

from dataclasses import dataclass
from typing import Any, Optional
from datetime import datetime, timedelta
import json


@dataclass
class ForemanConfig:
    """Configuration for Foreman orchestrator connection"""
    base_url: str
    api_key: str
    agent_id: str
    sync_interval_seconds: int = 5


@dataclass
class SyncState:
    """Track sync state with Foreman"""
    last_sync: Optional[datetime] = None
    last_sync_success: bool = True
    sync_failures: int = 0
    last_error: Optional[str] = None
    pending_updates: list[dict] = None

    def __post_init__(self):
        if self.pending_updates is None:
            self.pending_updates = []


@dataclass
class TelemetryEvent:
    """Telemetry event to send to Foreman"""
    event_type: str
    timestamp: datetime
    agent_id: str
    data: dict

    def to_dict(self) -> dict:
        return {
            "eventType": self.event_type,
            "timestamp": self.timestamp.isoformat(),
            "agentId": self.agent_id,
            "data": self.data,
        }


class SyncManager:
    """
    Manages synchronization with Foreman orchestrator.

    Handles:
    - State sync (push agent state to Foreman)
    - Mission retrieval (pull missions from Foreman)
    - Telemetry logging (send events to Foreman)
    - Heartbeat (keep-alive signals)
    """

    def __init__(self, config: ForemanConfig):
        self.config = config
        self.sync_state = SyncState()

    async def should_sync(self) -> bool:
        """
        Check if sync is needed based on time interval.

        Returns:
            True if sync should be performed
        """
        if self.sync_state.last_sync is None:
            return True

        elapsed = datetime.utcnow() - self.sync_state.last_sync
        return elapsed.total_seconds() >= self.config.sync_interval_seconds

    async def sync_state(self, agent_state: dict) -> dict:
        """
        Synchronize agent state with Foreman.

        Args:
            agent_state: Current agent state

        Returns:
            Response from Foreman with any updates
        """
        self.sync_state.last_sync = datetime.utcnow()

        try:
            # Prepare sync payload
            payload = {
                "agentId": self.config.agent_id,
                "timestamp": datetime.utcnow().isoformat(),
                "state": agent_state,
                "pendingUpdates": self.sync_state.pending_updates,
            }

            # Send to Foreman
            # (In actual implementation, use httpx or similar)
            # response = await self._send_to_foreman("/api/agents/sync", payload)

            # For now, simulate successful sync
            response = {
                "status": "synced",
                "missions": [],
                "configUpdates": {},
            }

            # Reset pending updates
            self.sync_state.pending_updates = []
            self.sync_state.last_sync_success = True
            self.sync_state.sync_failures = 0
            self.sync_state.last_error = None

            return response

        except Exception as e:
            self.sync_state.last_sync_success = False
            self.sync_state.sync_failures += 1
            self.sync_state.last_error = str(e)

            # Queue for retry
            self.sync_state.pending_updates.append({
                "timestamp": datetime.utcnow().isoformat(),
                "state": agent_state,
            })

            # Keep only last 10 pending updates
            if len(self.sync_state.pending_updates) > 10:
                self.sync_state.pending_updates = self.sync_state.pending_updates[-10:]

            raise

    async def get_missions(self) -> list[dict]:
        """
        Retrieve missions from Foreman.

        Returns:
            List of missions assigned to this agent
        """
        try:
            # Prepare request
            params = {
                "agentId": self.config.agent_id,
                "limit": 10,
            }

            # Request from Foreman
            # response = await self._get_from_foreman("/api/agents/missions", params)

            # For now, simulate empty missions
            missions = []

            return missions

        except Exception as e:
            self.sync_state.last_error = str(e)
            return []

    async def send_telemetry(self, events: list[TelemetryEvent]) -> bool:
        """
        Send telemetry events to Foreman.

        Args:
            events: List of telemetry events

        Returns:
            True if successful, False otherwise
        """
        if not events:
            return True

        try:
            # Prepare telemetry payload
            payload = {
                "agentId": self.config.agent_id,
                "events": [e.to_dict() for e in events],
            }

            # Send to Foreman
            # await self._send_to_foreman("/api/agents/telemetry", payload)

            return True

        except Exception as e:
            self.sync_state.last_error = str(e)
            return False

    async def send_heartbeat(self) -> bool:
        """
        Send heartbeat to Foreman to keep agent alive.

        Returns:
            True if successful, False otherwise
        """
        try:
            payload = {
                "agentId": self.config.agent_id,
                "timestamp": datetime.utcnow().isoformat(),
                "status": "alive",
            }

            # Send to Foreman
            # await self._send_to_foreman("/api/agents/heartbeat", payload)

            return True

        except Exception as e:
            self.sync_state.last_error = str(e)
            return False

    async def report_mission_complete(self, mission_id: str, result: dict) -> bool:
        """
        Report mission completion to Foreman.

        Args:
            mission_id: Completed mission ID
            result: Mission result data

        Returns:
            True if successful, False otherwise
        """
        try:
            payload = {
                "agentId": self.config.agent_id,
                "missionId": mission_id,
                "timestamp": datetime.utcnow().isoformat(),
                "result": result,
            }

            # Send to Foreman
            # await self._send_to_foreman("/api/agents/missions/complete", payload)

            return True

        except Exception as e:
            self.sync_state.last_error = str(e)
            return False

    async def report_mission_failed(self, mission_id: str, error: str) -> bool:
        """
        Report mission failure to Foreman.

        Args:
            mission_id: Failed mission ID
            error: Error message

        Returns:
            True if successful, False otherwise
        """
        try:
            payload = {
                "agentId": self.config.agent_id,
                "missionId": mission_id,
                "timestamp": datetime.utcnow().isoformat(),
                "error": error,
            }

            # Send to Foreman
            # await self._send_to_foreman("/api/agents/missions/failed", payload)

            return True

        except Exception as e:
            self.sync_state.last_error = str(e)
            return False

    def get_sync_status(self) -> dict:
        """
        Get current sync status.

        Returns:
            Sync status information
        """
        return {
            "lastSync": self.sync_state.last_sync.isoformat() if self.sync_state.last_sync else None,
            "lastSuccess": self.sync_state.last_sync_success,
            "failures": self.sync_state.sync_failures,
            "lastError": self.sync_state.last_error,
            "pendingUpdates": len(self.sync_state.pending_updates),
        }


class TelemetryLogger:
    """
    Buffers and batches telemetry events for efficient transmission.
    """

    def __init__(self, agent_id: str, buffer_size: int = 100):
        self.agent_id = agent_id
        self.buffer_size = buffer_size
        self.buffer: list[TelemetryEvent] = []

    def log(self, event_type: str, data: dict) -> TelemetryEvent:
        """
        Log a telemetry event.

        Args:
            event_type: Type of event (e.g., "tactical", "combat", "mission")
            data: Event data

        Returns:
            Created telemetry event
        """
        event = TelemetryEvent(
            event_type=event_type,
            timestamp=datetime.utcnow(),
            agent_id=self.agent_id,
            data=data,
        )

        self.buffer.append(event)

        # Trim buffer if needed
        if len(self.buffer) > self.buffer_size:
            self.buffer = self.buffer[-self.buffer_size:]

        return event

    def get_events(
        self,
        event_type: Optional[str] = None,
        since: Optional[datetime] = None,
        limit: int = 100,
    ) -> list[TelemetryEvent]:
        """
        Retrieve buffered events.

        Args:
            event_type: Filter by event type
            since: Filter events after this timestamp
            limit: Maximum number of events to return

        Returns:
            List of matching events
        """
        events = self.buffer

        # Filter by type
        if event_type:
            events = [e for e in events if e.event_type == event_type]

        # Filter by time
        if since:
            events = [e for e in events if e.timestamp >= since]

        # Sort by timestamp (newest first) and limit
        events.sort(key=lambda e: e.timestamp, reverse=True)
        return events[:limit]

    def clear_events(self, before: Optional[datetime] = None) -> int:
        """
        Clear events from buffer.

        Args:
            before: Clear events before this timestamp (clears all if None)

        Returns:
            Number of events cleared
        """
        if before is None:
            count = len(self.buffer)
            self.buffer = []
            return count

        old_count = len(self.buffer)
        self.buffer = [e for e in self.buffer if e.timestamp >= before]
        return old_count - len(self.buffer)

    def get_summary(self) -> dict:
        """
        Get telemetry summary.

        Returns:
            Summary statistics
        """
        if not self.buffer:
            return {
                "totalEvents": 0,
                "eventTypes": {},
                "oldestEvent": None,
                "newestEvent": None,
            }

        event_types = {}
        for event in self.buffer:
            event_types[event.event_type] = event_types.get(event.event_type, 0) + 1

        sorted_events = sorted(self.buffer, key=lambda e: e.timestamp)

        return {
            "totalEvents": len(self.buffer),
            "eventTypes": event_types,
            "oldestEvent": sorted_events[0].timestamp.isoformat(),
            "newestEvent": sorted_events[-1].timestamp.isoformat(),
        }


async def create_sync_config(env: Any) -> ForemanConfig:
    """
    Create Foreman configuration from environment variables.

    Args:
        env: Cloudflare Worker environment

    Returns:
        Foreman configuration
    """
    # Get configuration from environment
    base_url = env.FOREMAN_URL if hasattr(env, "FOREMAN_URL") else "http://localhost:8080"
    api_key = env.FOREMAN_API_KEY if hasattr(env, "FOREMAN_API_KEY") else ""
    agent_id = env.AGENT_ID if hasattr(env, "AGENT_ID") else "unknown"

    # Get sync interval from env vars
    sync_interval = int(env.SYNC_INTERVAL_SECONDS) if hasattr(env, "SYNC_INTERVAL_SECONDS") else 5

    return ForemanConfig(
        base_url=base_url,
        api_key=api_key,
        agent_id=agent_id,
        sync_interval_seconds=sync_interval,
    )


async def perform_periodic_sync(
    sync_manager: SyncManager,
    agent_state: dict,
    telemetry_logger: TelemetryLogger,
) -> dict:
    """
    Perform periodic sync with Foreman.

    Args:
        sync_manager: Sync manager instance
        agent_state: Current agent state
        telemetry_logger: Telemetry logger instance

    Returns:
        Sync result with status and any updates
    """
    result = {
        "synced": False,
        "missions": [],
        "configUpdates": {},
        "telemetrySent": False,
    }

    # Check if sync is needed
    if not await sync_manager.should_sync():
        return result

    # Sync state
    sync_response = await sync_manager.sync_state(agent_state)
    result["synced"] = True
    result["missions"] = sync_response.get("missions", [])
    result["configUpdates"] = sync_response.get("configUpdates", {})

    # Get new missions
    if missions := await sync_manager.get_missions():
        result["missions"].extend(missions)

    # Send telemetry
    if telemetry_logger.buffer:
        telemetry_sent = await sync_manager.send_telemetry(telemetry_logger.buffer)
        result["telemetrySent"] = telemetry_sent

        # Clear sent events
        if telemetry_sent:
            telemetry_logger.clear_events()

    return result
