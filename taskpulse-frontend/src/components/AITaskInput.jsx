import { useState } from "react";
import {
    createAITask,
    confirmAITask
} from "../services/api";
function AITaskInput({ onTaskCreated }) {
    const [prompt, setPrompt] = useState("");
    const [preview, setPreview] = useState(null);
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState("");
    const handleGenerate = async () => {
        if (!prompt.trim()) {
            setError(
                "Please describe what you need to do."
            );
            return;
        }
        setLoading(true);
        setError("");
        setPreview(null);
        try {
            const response =
                await createAITask(
                    prompt.trim()
                );
            console.log(
                "Gemini extracted task:",
                response
            );
            setPreview(response);
        } catch (err) {
            console.error(
                "AI extraction failed:",
                err
            );
            setError(
                err.response?.data?.message ||
                "AI could not extract the task."
            );
        } finally {
            setLoading(false);
        }
    };
    const handlePreviewChange = (field, value) => {
        setPreview((current) => ({
            ...current,
            [field]: value
        }));
    };
    const handleConfirm = async () => {
        if (!preview) {
            return;
        }
        if (!preview.title?.trim()) {
            setError(
                "Task title cannot be empty."
            );
            return;
        }
        setSaving(true);
        setError("");
        try {
            const confirmedTask = {
                title:
                    preview.title.trim(),
                description:
                    preview.description ||
                    null,
                priority:
                    preview.priority ||
                    "MEDIUM",
                deadline:
                    preview.deadline ||
                    null,
                estimatedDurationMinutes:
                    preview.estimatedDurationMinutes
                        ? Number(
                            preview.estimatedDurationMinutes
                        )
                        : null
            };
            const createdTask =
                await confirmAITask(
                    confirmedTask
                );
            console.log(
                "Task created:",
                createdTask
            );
            onTaskCreated(createdTask);
            setPrompt("");
            setPreview(null);
        } catch (err) {
            console.error(
                "Task creation failed:",
                err
            );
            setError(
                err.response?.data?.message ||
                "Failed to create task."
            );
        } finally {
            setSaving(false);
        }
    };
    const handleCancel = () => {
        setPreview(null);
        setError("");
    };
    const handleNewPrompt = () => {
        setPreview(null);
        setError("");
    };
    const getExtractionWarnings = () => {

        if (!preview) {
            return [];
        }

        const warnings = [];

        if (!preview.deadlineExpression) {
            warnings.push(
                "No deadline was detected."
            );
        } else if (!preview.deadline) {
            warnings.push(
                `The deadline "${preview.deadlineExpression}" could not be resolved.`
            );
        }

        if (
            preview.estimatedDurationMinutes === null ||
            preview.estimatedDurationMinutes === undefined
        ) {
            warnings.push(
                "No estimated duration was detected."
            );
        }

        return warnings;
    };

    const extractionWarnings =
        getExtractionWarnings();
    return (
        <section className="ai-task-input">
            {!preview ? (
                <>
                    <div className="ai-input-header">
                        <span className="ai-badge">
                            ✨ AI
                        </span>
                        <p>
                            Describe your task naturally
                            and TaskPulse will extract
                            the details.
                        </p>
                    </div>
                    <textarea
                        value={prompt}
                        onChange={(e) =>
                            setPrompt(e.target.value)
                        }
                        placeholder={
                            "Example: I need to finish my DBMS assignment " +
                            "by Friday. It is urgent and should take 2 hours."
                        }
                        rows={4}
                        disabled={
                            loading ||
                            saving
                        }
                    />
                    <button
                        onClick={handleGenerate}
                        disabled={
                            loading ||
                            saving
                        }
                    >
                        {loading
                            ? "✨ Gemini is thinking..."
                            : "✨ Generate Task"}
                    </button>
                </>
            ) : (
                <div className="ai-preview">
                    <div className="ai-preview-header">
                        <div>
                            <span className="ai-badge">
                                ✨ AI Generated
                            </span>
                            <h3>
                                Review your task
                            </h3>
                            <p>
                                Make any changes before
                                saving it.
                            </p>
                        </div>
                        <button
                            className="preview-new-button"
                            onClick={handleNewPrompt}
                            disabled={saving}
                        >
                            New
                        </button>
                    </div>
                    <div className="ai-preview-form">
                        {/* TITLE */}
                        <div className="form-group">
                            <label>
                                Title
                            </label>
                            <input
                                type="text"
                                value={
                                    preview.title || ""
                                }
                                onChange={(e) =>
                                    handlePreviewChange(
                                        "title",
                                        e.target.value
                                    )
                                }
                                maxLength={200}
                                disabled={saving}
                            />
                        </div>
                        {/* DESCRIPTION */}
                        <div className="form-group">
                            <label>
                                Description
                            </label>
                            <textarea
                                value={
                                    preview.description ||
                                    ""
                                }
                                onChange={(e) =>
                                    handlePreviewChange(
                                        "description",
                                        e.target.value
                                    )
                                }
                                maxLength={5000}
                                rows={3}
                                placeholder="Optional"
                                disabled={saving}
                            />
                        </div>
                        {/* PRIORITY + DURATION */}
                        <div className="form-row">
                            <div className="form-group">
                                <label>
                                    Priority
                                </label>
                                <select
                                    value={
                                        preview.priority ||
                                        "MEDIUM"
                                    }
                                    onChange={(e) =>
                                        handlePreviewChange(
                                            "priority",
                                            e.target.value
                                        )
                                    }
                                    disabled={saving}
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
                                <label>
                                    Duration (minutes)
                                </label>
                                <input
                                    type="number"
                                    min="1"
                                    max="1440"
                                    value={
                                        preview.estimatedDurationMinutes ??
                                        ""
                                    }
                                    onChange={(e) =>
                                        handlePreviewChange(
                                            "estimatedDurationMinutes",
                                            e.target.value
                                        )
                                    }
                                    placeholder="Optional"
                                    disabled={saving}
                                />
                            </div>
                        </div>
                        {/* DEADLINE */}
                        <div className="form-group">
                            <label>
                                Deadline
                            </label>
                            <input
                                type="datetime-local"
                                value={
                                    preview.deadline
                                        ? preview.deadline.slice(
                                            0,
                                            16
                                        )
                                        : ""
                                }
                                onChange={(e) =>
                                    handlePreviewChange(
                                        "deadline",
                                        e.target.value
                                            ? e.target.value
                                            : null
                                    )
                                }
                                disabled={saving}
                            />
                        </div>
                    </div>
                    {preview.deadlineExpression && (

                        <p className="deadline-source">

                            AI interpreted:

                            <strong>
                                {" "}
                                "{preview.deadlineExpression}"
                            </strong>

                        </p>

                    )}

                    {extractionWarnings.length > 0 && (

                        <div className="ai-warnings">

                            <div className="ai-warning-title">
                                ⚠ Please review
                            </div>

                            {extractionWarnings.map(
                                (warning, index) => (

                                    <p key={index}>
                                        {warning}
                                    </p>

                                )
                            )}

                        </div>

                    )}
                    <div className="preview-actions">
                        <button
                            className="confirm-task-button"
                            onClick={handleConfirm}
                            disabled={saving}
                        >
                            {saving
                                ? "Creating..."
                                : "✓ Create Task"}
                        </button>
                        <button
                            className="cancel-task-button"
                            onClick={handleCancel}
                            disabled={saving}
                        >
                            Cancel
                        </button>
                    </div>
                </div>
            )}
            {error && (
                <p className="error">
                    {error}
                </p>
            )}
        </section>
    );
}
export default AITaskInput;
