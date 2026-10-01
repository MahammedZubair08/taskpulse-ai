import { useState } from "react";
import { generateAISchedule, applyAISchedule } from "../services/api";
import "../styles/ai-schedule.css";

function AISchedulePanel({ onScheduleApplied }) {
    const [loading, setLoading] = useState(false);
    const [applying, setApplying] = useState(false);
    const [scheduleData, setScheduleData] = useState(null);
    const [exportCalendar, setExportCalendar] = useState(true);
    const [error, setError] = useState("");
    const [successMsg, setSuccessMsg] = useState("");

    const handleGenerate = async () => {
        try {
            setLoading(true);
            setError("");
            setSuccessMsg("");
            const data = await generateAISchedule();
            setScheduleData(data);
            if (!data.schedule || data.schedule.length === 0) {
                setSuccessMsg("No active tasks found to schedule!");
            }
        } catch (err) {
            console.error("Failed to generate AI schedule:", err);
            setError(err.response?.data?.message || "Failed to generate AI schedule. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    const handleApply = async () => {
        if (!scheduleData || !scheduleData.schedule) return;
        try {
            setApplying(true);
            setError("");
            const result = await applyAISchedule(scheduleData.schedule, exportCalendar);
            setSuccessMsg(`⚡ Successfully applied schedule to ${result.appliedCount} task(s)!${exportCalendar ? " Also exported to Google Calendar." : ""}`);
            if (onScheduleApplied) {
                onScheduleApplied();
            }
        } catch (err) {
            console.error("Failed to apply AI schedule:", err);
            setError("Failed to apply schedule to tasks.");
        } finally {
            setApplying(false);
        }
    };

    const formatTime = (isoString) => {
        if (!isoString) return "";
        const date = new Date(isoString);
        return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    };

    return (
        <section className="ai-schedule-section">
            <div className="ai-schedule-header">
                <div className="ai-schedule-title">
                    <span className="ai-schedule-icon">🤖</span>
                    <div>
                        <h2>Intelligent AI Schedule Engine</h2>
                        <p>Workload balancing & conflict-aware daily schedule optimization powered by Gemini</p>
                    </div>
                </div>

                <button
                    className="ai-schedule-generate-btn"
                    onClick={handleGenerate}
                    disabled={loading}
                >
                    {loading ? "✨ Optimizing Schedule..." : "✨ Optimize Today's Schedule"}
                </button>
            </div>

            {error && <div className="schedule-msg schedule-error">{error}</div>}
            {successMsg && <div className="schedule-msg schedule-success">{successMsg}</div>}

            {scheduleData && scheduleData.schedule && scheduleData.schedule.length > 0 && (
                <div className="schedule-results">
                    {scheduleData.overallStrategy && (
                        <div className="strategy-box">
                            <strong>💡 AI Strategy:</strong> {scheduleData.overallStrategy}
                        </div>
                    )}

                    <div className="schedule-timeline">
                        <h3>Proposed Timeline for Today ({scheduleData.schedule.length} Tasks)</h3>
                        <div className="timeline-list">
                            {scheduleData.schedule.map((block, idx) => (
                                <div key={block.taskId || idx} className="timeline-card">
                                    <div className="timeline-time">
                                        ⏱️ {formatTime(block.startTime)} - {formatTime(block.endTime)}
                                        <span className="timeline-duration">({block.durationMinutes} mins)</span>
                                    </div>
                                    <div className="timeline-content">
                                        <div className="timeline-title-row">
                                            <span className="timeline-task-title">{block.taskTitle}</span>
                                            <span className={`priority-tag priority-${block.priority?.toLowerCase() || 'medium'}`}>
                                                {block.priority || 'MEDIUM'}
                                            </span>
                                        </div>
                                        {block.reasoning && (
                                            <div className="timeline-reasoning">
                                                🧠 <em>{block.reasoning}</em>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>

                    <div className="schedule-apply-controls">
                        <label className="export-calendar-checkbox">
                            <input
                                type="checkbox"
                                checked={exportCalendar}
                                onChange={(e) => setExportCalendar(e.target.checked)}
                            />
                            📅 Export proposed schedule directly to Google Calendar
                        </label>

                        <button
                            className="apply-schedule-btn"
                            onClick={handleApply}
                            disabled={applying}
                        >
                            {applying ? "Applying..." : "⚡ Apply AI Schedule"}
                        </button>
                    </div>
                </div>
            )}
        </section>
    );
}

export default AISchedulePanel;
