import { useEffect, useState } from "react";
import {
    getCalendarEvents,
    getCalendarConflicts,
    getGmailStatus
} from "../services/api";
import "../styles/calendar-sync.css";

function CalendarSync() {
    const [connected, setConnected] = useState(false);
    const [events, setEvents] = useState([]);
    const [conflicts, setConflicts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        loadCalendarData();
    }, []);

    const loadCalendarData = async () => {
        try {
            setLoading(true);
            const status = await getGmailStatus(); // Uses same Google OAuth status
            setConnected(status.connected);

            if (status.connected) {
                const [eventList, conflictList] = await Promise.all([
                    getCalendarEvents(),
                    getCalendarConflicts()
                ]);
                setEvents(eventList || []);
                setConflicts(conflictList || []);
            }
        } catch (err) {
            console.error("Failed to load Google Calendar data:", err);
            setError("Could not load Google Calendar data.");
        } finally {
            setLoading(false);
        }
    };

    if (loading) {
        return <div className="calendar-sync-container loading">Loading Google Calendar schedule...</div>;
    }

    if (!connected) {
        return null; // Gmail/Google connect button in GmailSync handles initial auth
    }

    return (
        <section className="calendar-sync-section">
            <div className="calendar-header">
                <div className="calendar-title">
                    <span className="calendar-icon">📅</span>
                    <div>
                        <h2>Google Calendar & Schedule Overview</h2>
                        <p>Real-time conflict detection and calendar schedule sync</p>
                    </div>
                </div>

                <button className="calendar-refresh-btn" onClick={loadCalendarData}>
                    🔄 Refresh Schedule
                </button>
            </div>

            {error && <div className="calendar-error">{error}</div>}

            {/* CONFLICTS ALERT BANNER */}
            {conflicts.length > 0 && (
                <div className="conflicts-banner">
                    <div className="conflicts-banner-title">
                        ⚠️ <strong>{conflicts.length} Schedule Conflict(s) Detected!</strong>
                    </div>
                    <div className="conflicts-list">
                        {conflicts.map((conflict, idx) => (
                            <div key={idx} className="conflict-item">
                                <span className="conflict-task">Task: "{conflict.taskTitle}"</span>
                                <span className="conflict-vs">overlaps with</span>
                                <span className="conflict-event">Calendar Event: "{conflict.conflictingEventSummary}"</span>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {/* UPCOMING EVENTS LIST */}
            <div className="events-overview">
                <h3>Upcoming Calendar Events ({events.length})</h3>
                {events.length === 0 ? (
                    <p className="no-events">No upcoming calendar events found for the next 14 days.</p>
                ) : (
                    <div className="events-grid">
                        {events.slice(0, 4).map((evt) => {
                            const startDate = new Date(evt.start);
                            return (
                                <div key={evt.id} className="event-card">
                                    <div className="event-time">
                                        {evt.allDay ? "All Day" : startDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                    </div>
                                    <div className="event-summary">{evt.summary}</div>
                                    <div className="event-date">{startDate.toLocaleDateString()}</div>
                                </div>
                            );
                        })}
                    </div>
                )}
            </div>
        </section>
    );
}

export default CalendarSync;
