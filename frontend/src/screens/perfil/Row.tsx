import styles from "./Perfil.module.css";

export const Row = ({ label, value }: { label: string; value: string }) => (
  <div className={styles.row}>
    <span className={styles.rowLabel}>{label}</span>
    <span className={styles.rowValue}>{value}</span>
  </div>
);
