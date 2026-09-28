import { useAppForm } from "@/config/use-app-form";
import { LoginRequest, LoginRequestSchema } from "@/models/Login";
import { Link } from "wouter";
import styles from "./Login.module.css";

type Props = {
  onSubmit: (value: LoginRequest) => void;
  submitError: Error | null;
};

export function Login({ onSubmit, submitError }: Props) {
  const formData = useAppForm({
    defaultValues: {
      email: "",
      password: "",
    },
    validators: {
      onChange: LoginRequestSchema,
    },
    onSubmit: async ({ value }) => onSubmit(value),
  });

  return (
    <div className={styles.loginContainer}>
      <div className={styles.loginCard}>
        <span className={styles.logo}>TurnosYa</span>
        <h1 className={styles.title}>Log In</h1>

        <formData.AppForm>
          <formData.FormContainer extraError={submitError}>
            <formData.AppField
              name="email"
              children={(field) => <field.TextField label="Email" />}
            />
            <formData.AppField
              name="password"
              children={(field) => <field.PasswordField label="Contraseña" />}
            />
            <Link className={styles.forgotLink} href="/forgot-password">
              ¿Olvidaste tu contraseña?
            </Link>
          </formData.FormContainer>
        </formData.AppForm>
      </div>
    </div>
  );
}
