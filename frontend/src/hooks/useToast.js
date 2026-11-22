import { useState, useCallback } from "react";

let nextId = 0;

export function useToast() {
  const [toasts, setToasts] = useState([]);
  const show = useCallback((msg, type = "info") => {
    const id = ++nextId; // unique even for toasts shown in the same millisecond
    setToasts((prev) => [...prev, { id, msg, type }]);
    setTimeout(() => setToasts((prev) => prev.filter((t) => t.id !== id)), 4000);
  }, []);
  return { toasts, show };
}
