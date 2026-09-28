import { useState } from "react";
import styles from "../AgendaScreen.module.css";

type Props = {
  title: string;
  defaultOpen?: boolean;
  open?: boolean;
  onToggle?: (open: boolean) => void;
  children: React.ReactNode;
};

export const CollapsibleSection = ({
  title,
  defaultOpen,
  open: controlledOpen,
  onToggle,
  children,
}: Props) => {
  const [internalOpen, setInternalOpen] = useState(defaultOpen ?? true);
  const open = controlledOpen ?? internalOpen;
  const setOpen = onToggle ?? setInternalOpen;
  return (
    <section className={styles.card}>
      <button className={styles.collapsibleHeader} onClick={() => setOpen(!open)} type="button">
        <span className={styles.sectionTitle}>{title}</span>
        <span className={styles.collapseArrow} data-open={open}>
          {open ? "▲" : "▼"}
        </span>
      </button>
      {open && <div className={styles.collapsibleBody}>{children}</div>}
    </section>
  );
};
