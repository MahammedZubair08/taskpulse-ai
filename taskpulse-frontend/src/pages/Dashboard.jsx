import { useEffect, useState } from "react";
import AITaskInput from "../components/AITaskInput";
import ManualTaskInput from "../components/ManualTaskInput";
import GmailSync from "../components/GmailSync";
import TaskList from "../components/TaskList";
import { getTasks } from "../services/api";

import "../styles/dashboard.css";
function Dashboard({ onLogout }) {
    const [tasks, setTasks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [statusFilter, setStatusFilter] = useState("ALL");
    const [priorityFilter, setPriorityFilter] = useState("ALL");
    const [creationMode, setCreationMode] = useState("AI");
    const [search, setSearch] = useState("");
    const [sortBy, setSortBy] = useState("CREATED_DESC");
    const [deadlineFilter, setDeadlineFilter] = useState("ALL");
    
    useEffect(() => {
        loadTasks();
    }, []);
    const loadTasks = async () => {
        try {
            const data = await getTasks();
            setTasks(data);
        } catch (error) {
            console.error(
                "Failed to load tasks:",
                error
            );
        } finally {
            setLoading(false);
        }
    };
    const handleTaskCreated = (task) => {
        setTasks((currentTasks) => [
            task,
            ...currentTasks
        ]);
        setCreationMode("AI");
    };
    const handleTaskUpdated = (updatedTask) => {
        setTasks((currentTasks) =>
            currentTasks.map((task) =>
                task.id === updatedTask.id
                    ? updatedTask
                    : task
            )
        );
    };
    const handleTaskDeleted = (id) => {
        setTasks((currentTasks) =>
            currentTasks.filter(
                (task) => task.id !== id
            )
        );
    };
    const filteredTasks = tasks
        .filter((task) => {

            const statusMatches =
                statusFilter === "ALL" ||
                task.status === statusFilter;

            const priorityMatches =
                priorityFilter === "ALL" ||
                task.priority === priorityFilter;

            const searchText = search.toLowerCase().trim();

            const searchMatches =
                !searchText ||
                task.title?.toLowerCase().includes(searchText) ||
                task.description?.toLowerCase().includes(searchText);

            let deadlineMatches = true;

            if (deadlineFilter !== "ALL" && task.status !== "COMPLETED") {

                if (!task.deadline) {
                    deadlineMatches = false;
                } else {

                    const deadline = new Date(task.deadline);

                    if (deadlineFilter === "OVERDUE") {
                        deadlineMatches = deadline < now;
                    }

                    if (deadlineFilter === "TODAY") {
                        deadlineMatches =
                            deadline.getFullYear() === now.getFullYear() &&
                            deadline.getMonth() === now.getMonth() &&
                            deadline.getDate() === now.getDate();
                    }

                    if (deadlineFilter === "UPCOMING") {
                        deadlineMatches =
                            deadline > now &&
                            !todayTasks.some(
                                (todayTask) =>
                                    todayTask.id === task.id
                            );
                    }
                }
            }

            return (
                statusMatches &&
                priorityMatches &&
                searchMatches &&
                deadlineMatches
            );
        })
        .sort((a, b) => {

            switch (sortBy) {

                case "DEADLINE_ASC":
                    if (!a.deadline) return 1;
                    if (!b.deadline) return -1;

                    return (
                        new Date(a.deadline) -
                        new Date(b.deadline)
                    );

                case "PRIORITY_DESC": {

                    const priorityOrder = {
                        LOW: 1,
                        MEDIUM: 2,
                        HIGH: 3,
                        URGENT: 4
                    };

                    return (
                        priorityOrder[b.priority] -
                        priorityOrder[a.priority]
                    );
                }

                case "TITLE_ASC":
                    return (a.title || "")
                        .localeCompare(b.title || "");

                case "CREATED_ASC":
                    return new Date(a.createdAt) -
                        new Date(b.createdAt);

                case "CREATED_DESC":
                default:
                    return new Date(b.createdAt) -
                        new Date(a.createdAt);
            }
        });
    const total = tasks.length;
    const todo = tasks.filter(
        (task) => task.status === "TODO"
    ).length;
    const inProgress = tasks.filter(
        (task) => task.status === "IN_PROGRESS"
    ).length;
    const completed = tasks.filter(
        (task) => task.status === "COMPLETED"
    ).length;
    const now = new Date();

    const overdueTasks = tasks.filter((task) => {
        if (!task.deadline || task.status === "COMPLETED") {
            return false;
        }

        return new Date(task.deadline) < now;
    });

    const todayTasks = tasks.filter((task) => {
        if (!task.deadline || task.status === "COMPLETED") {
            return false;
        }

        const deadline = new Date(task.deadline);

        return (
            deadline.getFullYear() === now.getFullYear() &&
            deadline.getMonth() === now.getMonth() &&
            deadline.getDate() === now.getDate()
        );
    });

    const upcomingTasks = tasks.filter((task) => {
        if (!task.deadline || task.status === "COMPLETED") {
            return false;
        }

        return new Date(task.deadline) > now &&
            !todayTasks.some(
                (todayTask) => todayTask.id === task.id
            );
    });
    return (
        <main className="dashboard">
            {/* =========================
                HEADER
               ========================= */}
            <div className="dashboard-top">
                <div>
                    <h1>TaskPulse</h1>
                    <p>
                        Turn your thoughts into tasks.
                    </p>
                </div>
                <button
                    className="logout-button"
                    onClick={onLogout}
                >
                    Logout
                </button>
            </div>
            {/* =========================
                CREATE TASK
               ========================= */}
            <section className="create-task-section">
                <div className="create-task-heading">
                    <h2>Create Task</h2>
                    <p>
                        Choose how you want to create your task.
                    </p>
                </div>
                {/* TABS */}
                <div className="creation-tabs">
                    <button
                        className={
                            creationMode === "AI"
                                ? "creation-tab active"
                                : "creation-tab"
                        }
                        onClick={() =>
                            setCreationMode("AI")
                        }
                    >
                        ✨ Create with AI
                    </button>
                    <button
                        className={
                            creationMode === "MANUAL"
                                ? "creation-tab active"
                                : "creation-tab"
                        }
                        onClick={() =>
                            setCreationMode("MANUAL")
                        }
                    >
                        ＋ Manual Task
                    </button>
                </div>
                {/* SELECTED FORM */}
                {creationMode === "AI" ? (
                    <AITaskInput
                        onTaskCreated={handleTaskCreated}
                    />
                ) : (
                    <ManualTaskInput
                        onTaskCreated={handleTaskCreated}
                    />
                )}
            </section>

            {/* =========================
                GMAIL INTEGRATION
               ========================= */}
            <GmailSync onTaskCreated={handleTaskCreated} />
            {/* =========================
                STATISTICS
               ========================= */}
            <div className="stats">
                <div className="stat-card">
                    <strong>{total}</strong>
                    <span>Total</span>
                </div>
                <div className="stat-card">
                    <strong>{todo}</strong>
                    <span>Todo</span>
                </div>
                <div className="stat-card">
                    <strong>{inProgress}</strong>
                    <span>In Progress</span>
                </div>
                <div className="stat-card">
                    <strong>{completed}</strong>
                    <span>Completed</span>
                </div>
            </div>
            <section className="deadline-summary">

                <div className="deadline-summary-header">
                    <div>
                        <h2>Deadline Overview</h2>
                        <p>Stay on top of what needs your attention.</p>
                    </div>
                </div>

                <div className="deadline-cards">

                    <button
                        className={
                            `deadline-card overdue-card ${deadlineFilter === "OVERDUE"
                                ? "selected"
                                : ""
                            }`
                        }
                        onClick={() =>
                            setDeadlineFilter(
                                deadlineFilter === "OVERDUE"
                                    ? "ALL"
                                    : "OVERDUE"
                            )
                        }
                    >
                        <div className="deadline-card-icon">
                            !
                        </div>

                        <div>
                            <strong>{overdueTasks.length}</strong>
                            <span>Overdue</span>
                        </div>
                    </button>


                    <button
                        className={
                            `deadline-card today-card ${deadlineFilter === "TODAY"
                                ? "selected"
                                : ""
                            }`
                        }
                        onClick={() =>
                            setDeadlineFilter(
                                deadlineFilter === "TODAY"
                                    ? "ALL"
                                    : "TODAY"
                            )
                        }
                    >
                        <div className="deadline-card-icon">
                            ◷
                        </div>

                        <div>
                            <strong>{todayTasks.length}</strong>
                            <span>Due Today</span>
                        </div>
                    </button>


                    <button
                        className={
                            `deadline-card upcoming-card ${deadlineFilter === "UPCOMING"
                                ? "selected"
                                : ""
                            }`
                        }
                        onClick={() =>
                            setDeadlineFilter(
                                deadlineFilter === "UPCOMING"
                                    ? "ALL"
                                    : "UPCOMING"
                            )
                        }
                    >
                        <div className="deadline-card-icon">
                            →
                        </div>

                        <div>
                            <strong>{upcomingTasks.length}</strong>
                            <span>Upcoming</span>
                        </div>
                    </button>

                </div>

            </section>
            {/* =========================
                TASK LIST
               ========================= */}
            <section className="your-tasks">
                {deadlineFilter !== "ALL" && (
                    <button
                        className="active-deadline-filter"
                        onClick={() => setDeadlineFilter("ALL")}
                    >
                        Showing: {
                            deadlineFilter === "OVERDUE"
                                ? "Overdue"
                                : deadlineFilter === "TODAY"
                                    ? "Due Today"
                                    : "Upcoming"
                        }
                        <span>×</span>
                    </button>
                )}
                <div className="task-section-header">
                    <div>
                        <h2>Your Tasks</h2>
                        <span className="task-count">
                            {filteredTasks.length} tasks
                        </span>
                    </div>
                    <div className="task-controls">

                        <input
                            type="text"
                            className="task-search"
                            placeholder="Search tasks..."
                            value={search}
                            onChange={(e) =>
                                setSearch(e.target.value)
                            }
                        />

                        <select
                            value={statusFilter}
                            onChange={(e) =>
                                setStatusFilter(e.target.value)
                            }
                        >
                            <option value="ALL">
                                All Status
                            </option>

                            <option value="TODO">
                                Todo
                            </option>

                            <option value="IN_PROGRESS">
                                In Progress
                            </option>

                            <option value="COMPLETED">
                                Completed
                            </option>
                        </select>

                        <select
                            value={priorityFilter}
                            onChange={(e) =>
                                setPriorityFilter(e.target.value)
                            }
                        >
                            <option value="ALL">
                                All Priority
                            </option>

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

                        <select
                            value={sortBy}
                            onChange={(e) =>
                                setSortBy(e.target.value)
                            }
                        >
                            <option value="CREATED_DESC">
                                Newest
                            </option>

                            <option value="CREATED_ASC">
                                Oldest
                            </option>

                            <option value="DEADLINE_ASC">
                                Deadline
                            </option>

                            <option value="PRIORITY_DESC">
                                Priority
                            </option>

                            <option value="TITLE_ASC">
                                Title A-Z
                            </option>
                        </select>

                    </div>
                </div>
                {loading ? (
                    <div className="tasks-loading">
                        Loading your tasks...
                    </div>
                ) : (
                    <TaskList
                        tasks={filteredTasks}
                        onTaskUpdated={handleTaskUpdated}
                        onTaskDeleted={handleTaskDeleted}
                    />
                )}
            </section>
        </main>
    );
}
export default Dashboard;
