import { useState } from "react";
import { updateTask, deleteTask, exportTaskToCalendar } from "../services/api";
function TaskCard({
    task,
    onTaskUpdated,
    onTaskDeleted
}) {
    const [editing, setEditing] = useState(false);
    const [loading, setLoading] = useState(false);
    const [form, setForm] = useState({
        title: task.title || "",
        description: task.description || "",
        status: task.status || "TODO",
        priority: task.priority || "MEDIUM",
        deadline: task.deadline
            ? task.deadline.slice(0, 16)
            : "",
        estimatedDurationMinutes:
            task.estimatedDurationMinutes ?? ""
    });
    const handleStatusChange = async (e) => {
        const status = e.target.value;
        setLoading(true);
        try {
            const updatedTask = await updateTask(
                task.id,
                { status }
            );
            onTaskUpdated(updatedTask);
        } catch (error) {
            console.error(error);
            alert(
                error.response?.data?.message ||
                "Failed to update task status."
            );
        } finally {
            setLoading(false);
        }
    };
    const handleChange = (e) => {
        const { name, value } = e.target;
        setForm((current) => ({
            ...current,
            [name]: value
        }));
    };
    const handleEditStart = () => {
        setForm({
            title: task.title || "",
            description: task.description || "",
            status: task.status || "TODO",
            priority: task.priority || "MEDIUM",
            deadline: task.deadline
                ? task.deadline.slice(0, 16)
                : "",
            estimatedDurationMinutes:
                task.estimatedDurationMinutes ?? ""
        });
        setEditing(true);
    };
    const handleSave = async () => {
        if (!form.title.trim()) {
            alert("Title cannot be empty.");
            return;
        }
        setLoading(true);
        try {
            const updatedTask = await updateTask(
                task.id,
                {
                    title: form.title.trim(),
                    description:
                        form.description.trim() || null,
                    status: form.status,
                    priority: form.priority,
                    deadline:
                        form.deadline || null,
                    estimatedDurationMinutes:
                        form.estimatedDurationMinutes === ""
                            ? null
                            : Number(
                                form.estimatedDurationMinutes
                            )
                }
            );
            onTaskUpdated(updatedTask);
            setEditing(false);
        } catch (error) {
            console.error(error);
            alert(
                error.response?.data?.message ||
                "Failed to update task."
            );
        } finally {
            setLoading(false);
        }
    };
    const handleDelete = async () => {
        if (!window.confirm("Delete this task?")) {
            return;
        }
        setLoading(true);
        try {
            await deleteTask(task.id);
            onTaskDeleted(task.id);
        } catch (error) {
            console.error(error);
            alert(
                error.response?.data?.message ||
                "Failed to delete task."
            );
        } finally {
            setLoading(false);
        }
    };
    const formatDeadline = (deadline) => {
        if (!deadline) {
            return null;
        }
        const date = new Date(deadline);
        if (Number.isNaN(date.getTime())) {
            return deadline;
        }
        return date.toLocaleString();
    };
    const getStatusLabel = (status) => {
        switch (status) {
            case "TODO":
                return "To Do";
            case "IN_PROGRESS":
                return "In Progress";
            case "COMPLETED":
                return "Completed";
            default:
                return status;
        }
    };
    const getDeadlineState = (deadline, status) => {
        if (!deadline || status === "COMPLETED") {
            return null;
        }

        const deadlineDate = new Date(deadline);
        const now = new Date();

        if (deadlineDate < now) {
            return "OVERDUE";
        }

        const today = new Date();

        const isToday =
            deadlineDate.getFullYear() === today.getFullYear() &&
            deadlineDate.getMonth() === today.getMonth() &&
            deadlineDate.getDate() === today.getDate();

        if (isToday) {
            return "TODAY";
        }

        return "UPCOMING";
    };

    const deadlineState = getDeadlineState(
        task.deadline,
        task.status
    );
    if (editing) {
        return (
            <article className="task-card editing">
                <h3>Edit Task</h3>
                <div className="edit-form">
                    <label>
                        Title
                    </label>
                    <input
                        type="text"
                        name="title"
                        value={form.title}
                        onChange={handleChange}
                        maxLength={200}
                        disabled={loading}
                    />
                    <label>
                        Description
                    </label>
                    <textarea
                        name="description"
                        value={form.description}
                        onChange={handleChange}
                        maxLength={5000}
                        rows={4}
                        disabled={loading}
                    />
                    <label>
                        Status
                    </label>
                    <select
                        name="status"
                        value={form.status}
                        onChange={handleChange}
                        disabled={loading}
                    >
                        <option value="TODO">
                            To Do
                        </option>
                        <option value="IN_PROGRESS">
                            In Progress
                        </option>
                        <option value="COMPLETED">
                            Completed
                        </option>
                    </select>
                    <label>
                        Priority
                    </label>
                    <select
                        name="priority"
                        value={form.priority}
                        onChange={handleChange}
                        disabled={loading}
                    >
                        <option value="LOW">
                            Low
                        </option>
                        <option value="MEDIUM">
                            Medium
                        </option>
                        <option value="HIGH">
                            High
                        </option>
                        <option value="URGENT">
                            Urgent
                        </option>
                    </select>
                    <label>
                        Deadline
                    </label>
                    <input
                        type="datetime-local"
                        name="deadline"
                        value={form.deadline}
                        onChange={handleChange}
                        disabled={loading}
                    />
                    <label>
                        Estimated Duration (minutes)
                    </label>
                    <input
                        type="number"
                        name="estimatedDurationMinutes"
                        value={
                            form.estimatedDurationMinutes
                        }
                        onChange={handleChange}
                        min="1"
                        max="1440"
                        disabled={loading}
                    />
                </div>
                <div className="task-actions">
                    <button
                        onClick={handleSave}
                        disabled={loading}
                    >
                        {loading
                            ? "Saving..."
                            : "Save Changes"}
                    </button>
                    <button
                        onClick={() =>
                            setEditing(false)
                        }
                        disabled={loading}
                    >
                        Cancel
                    </button>
                </div>
            </article>
        );
    }

    const handleExportToCalendar = async () => {
        setLoading(true);
        try {
            await exportTaskToCalendar(task.id);
            alert(`✨ Task "${task.title}" exported to Google Calendar!`);
        } catch (error) {
            console.error(error);
            alert(
                error.response?.data?.message ||
                "Failed to export task to Google Calendar. Make sure Google Calendar is connected."
            );
        } finally {
            setLoading(false);
        }
    };

    return (
        <article
            className={
                `task-card ${task.status === "COMPLETED"
                    ? "completed"
                    : ""
                }`
            }
        >
            <div className="task-header">
                <div className="task-title-area">
                    <h3>{task.title}</h3>
                </div>
                <span
                    className={
                        `priority priority-${task.priority?.toLowerCase() ||
                        "medium"
                        }`
                    }
                >
                    {task.priority || "MEDIUM"}
                </span>
            </div>
            {task.description && (
                <p className="task-description">
                    {task.description}
                </p>
            )}
            <div className="task-details">
                {task.deadline && (
                    <span
                        className={
                            `task-detail deadline-detail ${deadlineState?.toLowerCase() || ""
                            }`
                        }
                    >
                        <strong>
                            {deadlineState === "OVERDUE"
                                ? "Overdue:"
                                : deadlineState === "TODAY"
                                    ? "Due today:"
                                    : "Deadline:"}
                        </strong>{" "}
                        {formatDeadline(task.deadline)}
                    </span>
                )}
                {task.estimatedDurationMinutes && (
                    <span className="task-detail">
                        <strong>Duration:</strong>{" "}
                        {task.estimatedDurationMinutes} min
                    </span>
                )}
            </div>
            <div className="task-status-control">
                <label>
                    Status
                </label>
                <select
                    value={task.status}
                    onChange={handleStatusChange}
                    disabled={loading}
                    className={
                        `status-select status-${task.status?.toLowerCase()
                        }`
                    }
                >
                    <option value="TODO">
                        To Do
                    </option>
                    <option value="IN_PROGRESS">
                        In Progress
                    </option>
                    <option value="COMPLETED">
                        Completed
                    </option>
                </select>
            </div>
            <div className="task-actions">
                <button
                    className="calendar-export-button"
                    onClick={handleExportToCalendar}
                    disabled={loading}
                    title="Export task event to Google Calendar"
                >
                    📅 Export to Calendar
                </button>
                <button
                    onClick={handleEditStart}
                    disabled={loading}
                >
                    Edit
                </button>
                <button
                    className="delete-button"
                    onClick={handleDelete}
                    disabled={loading}
                >
                    Delete
                </button>
            </div>
        </article>
    );
}
export default TaskCard;
