import { initiales } from "../../utils/formatters";
import styles from "./Avatar.module.css";

type Props = {
  src?: string | null;
  nombre: string | null;
  apellido: string | null;
  size?: number;
  className?: string;
};

export const Avatar = ({ src, nombre, apellido, size = 40, className = "" }: Props) => {
  if (src) {
    return (
      <div
        className={`${styles.avatar} ${className}`}
        style={{ width: size, height: size }}
      >
        <img src={src} alt="" className={styles.img} />
      </div>
    );
  }
  return (
    <div
      className={`${styles.avatar} ${className}`}
      style={{ width: size, height: size, fontSize: Math.max(size * 0.42, 11) }}
    >
      {initiales(nombre, apellido)}
    </div>
  );
};
