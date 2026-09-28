import { useState } from "react";

type Notification = { id: number; text: string; type: "success" | "error" };
let notifId = 0;

export function useNotification() {
  const [notifs, setNotifs] = useState<Notification[]>([]);
  const notify = (text: string, type: "success" | "error") => {
    const id = ++notifId;
    setNotifs((prev) => [...prev, { id, text, type }]);
    setTimeout(() => setNotifs((prev) => prev.filter((n) => n.id !== id)), 4000);
  };
  return { notifs, notify };
}
