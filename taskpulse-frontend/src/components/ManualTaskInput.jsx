import { useState } from "react";
import { createTask } from "../services/api";
import "../styles/manual-task.css";
function ManualTaskInput({ onTaskCreated }) {
    const [form, setForm] = useState({
        title: "",
        description: "",
        priority: "MEDIUM",
        deadline: "",
        estimatedDurationMinutes: ""
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const handleChange = (e) => {
        const { name, value } = e.target;
        setForm((current) => ({
            ...current,
            [name]: value
        }));
    };
    const handleSubmit = async (e) => {
        e.preventDefault();
        setError("");
        if (!form.title.trim()) {
            setError("Title is required.");
            return;
        }
        setLoading(true);
        try {
            const task = await createTask({
                title: form.title.trim(),
                description:
                    form.description.trim() || null,
                priority: form.priority,
                deadline:
                    form.deadline || null,
                estimatedDurationMinutes:
                    form.estimatedDurationMinutes === ""
                        ? null
                        : Number(
                            form.estimatedDurationMinutes
                        )
            });
            onTaskCreated(task);
            setForm({
                title: "",
                description: "",
                priority: "MEDIUM",
                deadline: "",
                estimatedDurationMinutes: ""
            });
        } catch (error) {
            console.error(error);
            setError(
                error.response?.data?.message ||
                "Failed to create task."
            );
        } finally {
            setLoading(false);
        }
    };
    return (
        <section className="manual-task">
            <div className="manual-task-header">
                <h2>Create Task Manually</h2>
                <p>
                    Add a task yourself without AI.
                </p>
            </div>
            <form
                className="manual-task-form"
                onSubmit={handleSubmit}
            >
                <div className="form-group full">
                    <label htmlFor="manual-title">
                        Title
                    </label>
                    <input
                        id="manual-title"
                        name="title"
                        type="text"
                        value={form.title}
                        onChange={handleChange}
                        placeholder="Enter task title"
                        maxLength={200}
                        disabled={loading}
                    />
                </div>
                <div className="form-group full">
                    <label htmlFor="manual-description">
                        Description
                    </label>
                    <textarea
                        id="manual-description"
                        name="description"
                        value={form.description}
                        onChange={handleChange}
                        placeholder="Add details about this task..."
                        rows={4}
                        maxLength={5000}
                        disabled={loading}
                    />
                </div>
                <div className="form-group">
                    <label htmlFor="manual-priority">
                        Priority
                    </label>
                    <select
                        id="manual-priority"
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
                </div>
                <div className="form-group">
                    <label htmlFor="manual-duration">
                        Duration (minutes)
                    </label>
                    <input
                        id="manual-duration"
                        name="estimatedDurationMinutes"
                        type="number"
                        min="1"
                        max="1440"
                        value={
                            form.estimatedDurationMinutes
                        }
                        onChange={handleChange}
                        placeholder="e.g. 60"
                        disabled={loading}
                    />
                </div>
                <div className="form-group full">
                    <label htmlFor="manual-deadline">
                        Deadline
                    </label>
                    <input
                        id="manual-deadline"
                        name="deadline"
                        type="datetime-local"
                        value={form.deadline}
                        onChange={handleChange}
                        disabled={loading}
                    />
                </div>
                {error && (
                    <p className="manual-task-error">
                        {error}
                    </p>
                )}
                <div className="manual-task-actions">
                    <button
                        type="submit"
                        className="manual-create-button"
                        disabled={loading}
                    >
                        {loading
                            ? "Creating..."
                            : "Create Task"}
                    </button>
                </div>
            </form>
        </section>
    );
}
export default ManualTaskInput;
