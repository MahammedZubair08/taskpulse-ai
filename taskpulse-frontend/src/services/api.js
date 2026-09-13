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

export default api;

