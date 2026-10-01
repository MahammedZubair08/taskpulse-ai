import axios from "axios";
const api = axios.create({
    baseURL: "http://localhost:8080/api",
    headers: {
        "Content-Type": "application/json"
    }
});
api.interceptors.request.use((config) => {
    const token = localStorage.getItem("token");
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});
export const login = async (email, password) => {
    const response = await api.post(
        "/auth/login",
        {
            email,
            password
        }
    );
    return response.data;
};
export const register = async (
    firstName,
    lastName,
    email,
    password
) => {
    const response = await api.post(
        "/auth/register",
        {
            firstName,
            lastName,
            email,
            password
        }
    );
    return response.data;
};
export const createTask = async (task) => {
    const response = await api.post(
        "/tasks",
        task
    );
    return response.data;
};
export const getTasks = async () => {
    const response = await api.get(
        "/tasks"
    );
    return response.data;
};
export const updateTask = async (id, task) => {
    const response = await api.put(
        `/tasks/${id}`,
        task
    );
    return response.data;
};
export const deleteTask = async (id) => {
    await api.delete(
        `/tasks/${id}`
    );
};
/*
 * Ask Gemini to extract task information.
 *
 * Backend:
 * POST /api/ai/extract-task
 */
export const createAITask = async (prompt) => {
    const response = await api.post(
        "/ai/extract-task",
        {
            prompt
        }
    );
    /*
     * Current backend returns:
     *
     * {
     *     result: "..."
     * }
     *
     * If your AI controller is returning the
     * structured AITaskResponse directly in the
     * future, this function can be simplified.
     */
    return response.data;
};
/*
 * Confirm the AI-generated task.
 *
 * Backend:
 * POST /api/ai/tasks
 */
export const confirmAITask = async (task) => {

    const response = await api.post(
        "/ai/tasks",
        task
    );

    return response.data;
};

/*
 * Gmail Integration Endpoints
 */
export const getGmailAuthUrl = async () => {
    const response = await api.get("/gmail/auth-url");
    return response.data;
};

export const getGmailStatus = async () => {
    const response = await api.get("/gmail/status");
    return response.data;
};

export const syncGmail = async () => {
    const response = await api.post("/gmail/sync");
    return response.data;
};

export const disconnectGmail = async () => {
    await api.delete("/gmail/disconnect");
};

/*
 * Google Calendar Integration Endpoints
 */
export const getCalendarEvents = async (from, to) => {
    const response = await api.get("/calendar/events", { params: { from, to } });
    return response.data;
};

export const getCalendarConflicts = async () => {
    const response = await api.get("/calendar/conflicts");
    return response.data;
};

export const getFreeTimeSlots = async (durationMinutes, deadline) => {
    const response = await api.get("/calendar/free-slots", {
        params: { durationMinutes, deadline }
    });
    return response.data;
};

export const exportTaskToCalendar = async (taskId, startTime) => {
    const response = await api.post(`/calendar/export/${taskId}`, { startTime });
    return response.data;
};

/*
 * AI Scheduling Engine Endpoints
 */
export const generateAISchedule = async (targetDate) => {
    const response = await api.post("/ai/schedule/generate", { targetDate });
    return response.data;
};

export const applyAISchedule = async (schedule, exportToGoogleCalendar = true) => {
    const response = await api.post("/ai/schedule/apply", { schedule, exportToGoogleCalendar });
    return response.data;
};

/*
 * Notification System Endpoints
 */
export const getNotifications = async () => {
    const response = await api.get("/notifications");
    return response.data;
};

export const getUnreadNotificationCount = async () => {
    const response = await api.get("/notifications/unread-count");
    return response.data;
};

export const markNotificationAsRead = async (id) => {
    const response = await api.put(`/notifications/${id}/read`);
    return response.data;
};

export const markAllNotificationsAsRead = async () => {
    const response = await api.put("/notifications/read-all");
    return response.data;
};

export const deleteNotification = async (id) => {
    await api.delete(`/notifications/${id}`);
};

export const triggerDailySummary = async () => {
    const response = await api.post("/notifications/daily-summary");
    return response.data;
};

export default api;


/*
 * WhatsApp Integration Endpoints
 */
// Link the authenticated user's WhatsApp number
export const connectWhatsApp = async (phoneNumber) => {
    const response = await api.post(
        "/whatsapp/connect",
        { phoneNumber }
    );
    return response.data;
};
// Disconnect WhatsApp from the authenticated user
export const disconnectWhatsApp = async () => {
    await api.delete("/whatsapp/disconnect");
};
// Get pending WhatsApp task suggestions
export const getWhatsAppSuggestions = async () => {
    const response = await api.get("/whatsapp/suggestions");
    return response.data;
};
// Confirm a WhatsApp suggestion and create the task
export const confirmWhatsAppSuggestion = async (id, task) => {
    const response = await api.post(
        `/whatsapp/suggestions/${id}/confirm`,
        task
    );
    return response.data;
};
// Dismiss a WhatsApp task suggestion
export const dismissWhatsAppSuggestion = async (id) => {
    await api.delete(`/whatsapp/suggestions/${id}`);
};

