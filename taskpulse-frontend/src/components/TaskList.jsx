import TaskCard from "./TaskCard";
function TaskList({
    tasks,
    onTaskUpdated,
    onTaskDeleted
}) {
    if (!tasks || tasks.length === 0) {
        return (
            <div className="empty">
                No tasks yet. Create your first task with AI.
            </div>
        );
    }
    return (
        <div className="task-list">
            {tasks.map((task) => (
                <TaskCard
                    key={task.id}
                    task={task}
                    onTaskUpdated={onTaskUpdated}
                    onTaskDeleted={onTaskDeleted}
                />
            ))}
        </div>
    );
}
export default TaskList;
