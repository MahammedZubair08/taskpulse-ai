import { useEffect, useState, useRef } from "react";
import {
    getNotifications,
    getUnreadNotificationCount,
    markNotificationAsRead,
    markAllNotificationsAsRead,
    deleteNotification,
    triggerDailySummary
} from "../services/api";
import "../styles/notification-center.css";

export default function NotificationCenter() {
    const [isOpen, setIsOpen] = useState(false);
    const [notifications, setNotifications] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const [activeTab, setActiveTab] = useState("ALL");
    const [loading, setLoading] = useState(false);
    const [generatingSummary, setGeneratingSummary] = useState(false);
    const dropdownRef = useRef(null);

    // Initial load & 30-second polling for unread count
    useEffect(() => {
        fetchUnreadCount();
        const interval = setInterval(fetchUnreadCount, 30000);
        return () => clearInterval(interval);
    }, []);

    // Load full notification list when dropdown opens
    useEffect(() => {
        if (isOpen) {
            fetchNotifications();
        }
    }, [isOpen]);

    // Close dropdown on outside click
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
                setIsOpen(false);
            }
        };
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    const fetchUnreadCount = async () => {
        try {
            const data = await getUnreadNotificationCount();
            setUnreadCount(data.unreadCount || 0);
        } catch (err) {
            console.error("Failed to fetch unread notification count:", err);
        }
    };

    const fetchNotifications = async () => {
        setLoading(true);
        try {
            const data = await getNotifications();
            setNotifications(data);
            fetchUnreadCount();
        } catch (err) {
            console.error("Failed to fetch notifications:", err);
        } finally {
            setLoading(false);
        }
    };

    const handleMarkAsRead = async (id, isRead) => {
        if (isRead) return;
        try {
            await markNotificationAsRead(id);
            setNotifications((prev) =>
                prev.map((item) => (item.id === id ? { ...item, read: true } : item))
            );
            fetchUnreadCount();
        } catch (err) {
            console.error("Failed to mark notification as read:", err);
        }
    };

    const handleMarkAllAsRead = async () => {
        try {
            await markAllNotificationsAsRead();
            setNotifications((prev) => prev.map((item) => ({ ...item, read: true })));
            setUnreadCount(0);
        } catch (err) {
            console.error("Failed to mark all as read:", err);
        }
    };

    const handleDelete = async (e, id) => {
        e.stopPropagation();
        try {
            await deleteNotification(id);
            setNotifications((prev) => prev.filter((item) => item.id !== id));
            fetchUnreadCount();
        } catch (err) {
            console.error("Failed to delete notification:", err);
        }
    };

    const handleGenerateBriefing = async () => {
        setGeneratingSummary(true);
        try {
            const newNotif = await triggerDailySummary();
            setNotifications((prev) => [newNotif, ...prev]);
            fetchUnreadCount();
        } catch (err) {
            console.error("Failed to generate daily briefing:", err);
        } finally {
            setGeneratingSummary(false);
        }
    };

    const filteredNotifications = notifications.filter((item) => {
        if (activeTab === "UNREAD") return !item.read;
        if (activeTab === "OVERDUE") return item.type === "TASK_OVERDUE";
        if (activeTab === "SUMMARIES") return item.type === "DAILY_SUMMARY";
        return true;
    });

    const getTypeIcon = (type) => {
        switch (type) {
            case "DEADLINE_APPROACHING":
                return "⏳";
            case "TASK_OVERDUE":
                return "🚨";
            case "DAILY_SUMMARY":
                return "📊";
            default:
                return "🔔";
        }
    };

    const formatTimestamp = (ts) => {
        if (!ts) return "";
        const date = new Date(ts);
        return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }) +
            " · " + date.toLocaleDateString([], { month: "short", day: "numeric" });
    };

    return (
        <div className="notification-container" ref={dropdownRef}>
            <button
                className="notification-bell-btn"
                onClick={() => setIsOpen(!isOpen)}
                title="Notifications"
            >
                🔔
                {unreadCount > 0 && (
                    <span className="notification-badge">
                        {unreadCount > 99 ? "99+" : unreadCount}
                    </span>
                )}
            </button>

            {isOpen && (
                <div className="notification-dropdown">
                    <div className="notification-header">
                        <h3>
                            <span>🔔</span> Notifications
                        </h3>
                        <div className="notification-actions">
                            {unreadCount > 0 && (
                                <button className="notif-action-btn" onClick={handleMarkAllAsRead}>
                                    Mark all read
                                </button>
                            )}
                        </div>
                    </div>

                    <div className="notification-tabs">
                        <button
                            className={`notif-tab ${activeTab === "ALL" ? "active" : ""}`}
                            onClick={() => setActiveTab("ALL")}
                        >
                            All ({notifications.length})
                        </button>
                        <button
                            className={`notif-tab ${activeTab === "UNREAD" ? "active" : ""}`}
                            onClick={() => setActiveTab("UNREAD")}
                        >
                            Unread ({unreadCount})
                        </button>
                        <button
                            className={`notif-tab ${activeTab === "OVERDUE" ? "active" : ""}`}
                            onClick={() => setActiveTab("OVERDUE")}
                        >
                            Overdue
                        </button>
                        <button
                            className={`notif-tab ${activeTab === "SUMMARIES" ? "active" : ""}`}
                            onClick={() => setActiveTab("SUMMARIES")}
                        >
                            Briefings
                        </button>
                    </div>

                    <div style={{ padding: "0 12px" }}>
                        <button
                            className="notif-briefing-btn"
                            onClick={handleGenerateBriefing}
                            disabled={generatingSummary}
                        >
                            <span>✨</span>
                            {generatingSummary ? "Generating Briefing..." : "Generate Today's Briefing"}
                        </button>
                    </div>

                    <div className="notification-body">
                        {loading ? (
                            <div className="notification-empty">Loading notifications...</div>
                        ) : filteredNotifications.length === 0 ? (
                            <div className="notification-empty">
                                {activeTab === "UNREAD"
                                    ? "No unread notifications 🎉"
                                    : "No notifications found"}
                            </div>
                        ) : (
                            filteredNotifications.map((item) => (
                                <div
                                    key={item.id}
                                    className={`notification-item ${item.type} ${
                                        !item.read ? "unread" : ""
                                    }`}
                                    onClick={() => handleMarkAsRead(item.id, item.read)}
                                >
                                    <div className="notif-icon">{getTypeIcon(item.type)}</div>
                                    <div className="notif-content">
                                        <div className="notif-title">{item.title}</div>
                                        <div className="notif-msg">{item.message}</div>
                                        <div className="notif-footer">
                                            <span>{formatTimestamp(item.createdAt)}</span>
                                            <button
                                                className="notif-dismiss-btn"
                                                onClick={(e) => handleDelete(e, item.id)}
                                                title="Dismiss"
                                            >
                                                Dismiss
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            ))
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}
