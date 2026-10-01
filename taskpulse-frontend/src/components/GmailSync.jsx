import { useEffect, useState } from "react";
import {
    getGmailAuthUrl,
    getGmailStatus,
    syncGmail,
    disconnectGmail,
    createTask
} from "../services/api";
import "../styles/gmail-sync.css";

function GmailSync({ onTaskCreated }) {
    const [connected, setConnected] = useState(false);
    const [email, setEmail] = useState("");
    const [checkingStatus, setCheckingStatus] = useState(true);
    const [syncing, setSyncing] = useState(false);
    const [suggestions, setSuggestions] = useState([]);
    const [error, setError] = useState("");
    const [successMsg, setSuccessMsg] = useState("");

    useEffect(() => {
        checkStatus();

        const handleMessage = (event) => {
            if (event.data === "GMAIL_CONNECTED") {
                checkStatus();
                setSuccessMsg("Gmail connected successfully!");
            }
        };

        window.addEventListener("message", handleMessage);
        return () => window.removeEventListener("message", handleMessage);
    }, []);

    const checkStatus = async () => {
        try {
            setCheckingStatus(true);
            const data = await getGmailStatus();
            setConnected(data.connected);
            if (data.email) setEmail(data.email);
        } catch (err) {
            console.error("Failed to check Gmail status:", err);
        } finally {
            setCheckingStatus(false);
        }
    };

    const handleConnect = async () => {
        try {
            setError("");
            const data = await getGmailAuthUrl();
            const width = 600;
            const height = 700;
            const left = window.screen.width / 2 - width / 2;
            const top = window.screen.height / 2 - height / 2;

            window.open(
                data.url,
                "Gmail OAuth",
                `width=${width},height=${height},top=${top},left=${left}`
            );
        } catch (err) {
            console.error("Failed to get auth URL:", err);
            setError("Could not launch Google authentication.");
        }
    };

    const handleDisconnect = async () => {
        if (!window.confirm("Are you sure you want to disconnect Gmail?")) return;
        try {
            await disconnectGmail();
            setConnected(false);
            setSuggestions([]);
            setSuccessMsg("Disconnected Gmail.");
        } catch (err) {
            console.error("Failed to disconnect Gmail:", err);
            setError("Failed to disconnect Gmail.");
        }
    };

    const handleSync = async () => {
        try {
            setSyncing(true);
            setError("");
            setSuccessMsg("");
            const data = await syncGmail();

            const autoCreated = data.autoCreatedTasks || [];
            const suggested = data.suggestions || [];

            setSuggestions(suggested);

            // Pass auto-created high/urgent priority tasks to dashboard list immediately
            if (autoCreated.length > 0) {
                autoCreated.forEach((task) => {
                    if (onTaskCreated) onTaskCreated(task);
                });
            }

            if (autoCreated.length > 0 && suggested.length > 0) {
                setSuccessMsg(`⚡ Auto-created ${autoCreated.length} important task(s)! Found ${suggested.length} suggestion(s) for review.`);
            } else if (autoCreated.length > 0) {
                setSuccessMsg(`⚡ Auto-created ${autoCreated.length} important task(s) directly into your task list!`);
            } else if (suggested.length > 0) {
                setSuccessMsg(`Found ${suggested.length} task suggestion(s) for your review below.`);
            } else {
                setSuccessMsg("No new actionable tasks found in unread emails.");
            }
        } catch (err) {
            console.error("Failed to sync Gmail:", err);
            setError("Failed to fetch emails. Check your connection or reconnect Gmail.");
        } finally {
            setSyncing(false);
        }
    };

    const handleUpdateSuggestion = (index, field, value) => {
        setSuggestions((prev) => {
            const next = [...prev];
            next[index] = { ...next[index], [field]: value };
            return next;
        });
    };

    const handleConfirmTask = async (suggestionIndex) => {
        const item = suggestions[suggestionIndex];
        try {
            const newTask = await createTask({
                title: item.title,
                priority: item.priority || "MEDIUM",
                deadline: item.deadline || null,
                estimatedDurationMinutes: item.estimatedDurationMinutes || null,
                description: `Extracted from email: "${item.emailSubject}" from ${item.emailFrom}`
            });

            if (onTaskCreated) {
                onTaskCreated(newTask);
            }

            // Remove confirmed suggestion from list
            setSuggestions((prev) => prev.filter((_, idx) => idx !== suggestionIndex));
            setSuccessMsg(`Task "${item.title}" created successfully!`);
        } catch (err) {
            console.error("Failed to create task from suggestion:", err);
            setError("Failed to create task.");
        }
    };

    const handleDismissSuggestion = (suggestionIndex) => {
        setSuggestions((prev) => prev.filter((_, idx) => idx !== suggestionIndex));
    };

    if (checkingStatus) {
        return <div className="gmail-sync-container loading">Checking Gmail integration status...</div>;
    }

    return (
        <section className="gmail-sync-section">
            <div className="gmail-sync-header">
                <div className="gmail-sync-title">
                    <span className="gmail-icon">📧</span>
                    <div>
                        <h2>Gmail Task Ingestion</h2>
                        <p>Automatically extract tasks from your unread emails using Gemini AI</p>
                    </div>
                </div>

                <div className="gmail-sync-actions">
                    {connected ? (
                        <>
                            <button
                                className="gmail-btn sync-btn"
                                onClick={handleSync}
                                disabled={syncing}
                            >
                                {syncing ? "🔄 Analyzing Inbox..." : "✨ Sync Unread Emails"}
                            </button>
                            <button
                                className="gmail-btn disconnect-btn"
                                onClick={handleDisconnect}
                            >
                                Disconnect
                            </button>
                        </>
                    ) : (
                        <button
                            className="gmail-btn connect-btn"
                            onClick={handleConnect}
                        >
                            🔗 Connect Gmail Account
                        </button>
                    )}
                </div>
            </div>

            {error && <div className="gmail-message error-msg">{error}</div>}
            {successMsg && <div className="gmail-message success-msg">{successMsg}</div>}

            {connected && suggestions.length > 0 && (
                <div className="suggestions-container">
                    <h3>Suggested Tasks from Inbox ({suggestions.length})</h3>
                    <p className="suggestions-subtitle">
                        Review and edit task suggestions below before adding them to your task list.
                    </p>

                    <div className="suggestions-list">
                        {suggestions.map((item, index) => (
                            <div key={item.emailId || index} className="suggestion-card">
                                <div className="email-meta">
                                    <div className="email-subject">📩 {item.emailSubject}</div>
                                    <div className="email-from">From: {item.emailFrom}</div>
                                    <div className="email-snippet">{item.emailSnippet}</div>
                                </div>

                                <div className="suggestion-fields">
                                    <div className="field-group">
                                        <label>Action Title</label>
                                        <input
                                            type="text"
                                            value={item.title || ""}
                                            onChange={(e) =>
                                                handleUpdateSuggestion(index, "title", e.target.value)
                                            }
                                        />
                                    </div>

                                    <div className="field-row">
                                        <div className="field-group">
                                            <label>Priority</label>
                                            <select
                                                value={item.priority || "MEDIUM"}
                                                onChange={(e) =>
                                                    handleUpdateSuggestion(index, "priority", e.target.value)
                                                }
                                            >
                                                <option value="LOW">Low</option>
                                                <option value="MEDIUM">Medium</option>
                                                <option value="HIGH">High</option>
                                                <option value="URGENT">Urgent</option>
                                            </select>
                                        </div>

                                        <div className="field-group">
                                            <label>Duration (mins)</label>
                                            <input
                                                type="number"
                                                min="1"
                                                max="1440"
                                                value={item.estimatedDurationMinutes || ""}
                                                onChange={(e) =>
                                                    handleUpdateSuggestion(
                                                        index,
                                                        "estimatedDurationMinutes",
                                                        e.target.value ? parseInt(e.target.value) : null
                                                    )
                                                }
                                                placeholder="e.g. 30"
                                            />
                                        </div>
                                    </div>
                                </div>

                                <div className="suggestion-card-actions">
                                    <button
                                        className="confirm-btn"
                                        onClick={() => handleConfirmTask(index)}
                                    >
                                        ＋ Create Task
                                    </button>
                                    <button
                                        className="dismiss-btn"
                                        onClick={() => handleDismissSuggestion(index)}
                                    >
                                        Dismiss
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}
        </section>
    );
}

export default GmailSync;
