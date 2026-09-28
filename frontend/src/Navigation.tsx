import { Redirect, Route, Switch } from "wouter";

import { AdminScreen } from "@/screens/AdminScreen";
import { AgendaScreen } from "@/screens/AgendaScreen";
import { CalendarScreen } from "@/screens/CalendarScreen";
import { LoginScreen } from "@/screens/LoginScreen";
import { ProfesionalesScreen } from "@/screens/ProfesionalesScreen";
import { PedirTurnoScreen } from "@/screens/PedirTurnoScreen";
import { ProfesionalDetailScreen } from "@/screens/ProfesionalDetailScreen";
import { SignupScreen } from "@/screens/SignupScreen";
import { PaginaUsuario } from "@/screens/PaginaUsuario";
import { CambioPasswordLogueado } from "@/screens/CambioPasswordLogueado";
import { CambioEmailLogueado } from "@/screens/CambioEmailLogueado";
import { CambioProfesionLogueado } from "@/screens/CambioProfesionLogueado";
import { ForgotPasswordScreen } from "@/screens/ForgotPasswordScreen";
import { ResetPasswordScreen } from "@/screens/ResetPasswordScreen";
import { VerifyEmailScreen } from "@/screens/VerifyEmailScreen";
import { PendingVerificationScreen } from "@/screens/PendingVerificationScreen";
import { ServiciosScreen } from "@/screens/ServiciosScreen";
import { PagoMockScreen } from "@/screens/PagoMockScreen";
import { ReprogramarScreen } from "@/screens/ReprogramarScreen";
import { CrearTurnoScreen } from "@/screens/CrearTurnoScreen";

import { useToken } from "@/services/TokenContext";

export const Navigation = () => {
  const [tokenState] = useToken();

  switch (tokenState.state) {
    case "LOGGED_IN":
    case "REFRESHING": {
      const roles =
        tokenState.state === "LOGGED_IN"
          ? tokenState.tokens.roles
          : [];

      const isSuperAdmin =
        roles.includes("SUPER_ADMIN");

      const isProfesional =
        roles.includes("PROFESIONAL");

      const verified =
        tokenState.state === "LOGGED_IN"
          ? tokenState.tokens.verified
          : true;

      const homeRedirect =
        isSuperAdmin
          ? "/admin"
          : isProfesional
          ? "/agenda"
          : "/profesionales";

      if (!verified && !isSuperAdmin) {
        return (
          <Switch>
            <Route path="/verify-email" component={VerifyEmailScreen} />
            <Route>
              <PendingVerificationScreen />
            </Route>
          </Switch>
        );
      }

      return (
        <Switch>
          <Route path="/admin">
            <AdminScreen />
          </Route>

          <Route path="/agenda">
            <AgendaScreen />
          </Route>

          <Route path="/servicios">
            <ServiciosScreen />
          </Route>

          <Route path="/perfil">
            <PaginaUsuario />
          </Route>

          <Route path="/cambiar-password-logueado">
            <CambioPasswordLogueado />
          </Route>

          <Route path="/cambiar-email-logueado">
            <CambioEmailLogueado />
          </Route>

          <Route path="/cambiar-profesion-logueado">
            <CambioProfesionLogueado />
          </Route>

          <Route path="/profesionales">
            <ProfesionalesScreen />
          </Route>

          <Route path="/pedir-turno">
            <PedirTurnoScreen />
          </Route>

          <Route path="/profesional/:id">
            {(params) => (
              <ProfesionalDetailScreen
                profesionalId={Number(
                  params!.id
                )}
              />
            )}
          </Route>

          <Route path="/calendario">
            <CalendarScreen />
          </Route>

          <Route path="/pago/:turnoId">
            {(params) => <PagoMockScreen turnoId={Number(params!.turnoId)} />}
          </Route>
          <Route path="/reprogramar/:turnoId">
            {(params) => <ReprogramarScreen turnoId={Number(params!.turnoId)} />}
          </Route>
          <Route path="/crear-turno">
            <CrearTurnoScreen />
          </Route>
          <Route path="/forgot-password" component={ForgotPasswordScreen} />
          <Route path="/reset-password" component={ResetPasswordScreen} />
          <Route path="/verify-email" component={VerifyEmailScreen} />
          <Route>
            <Redirect
              href={homeRedirect}
            />
          </Route>
        </Switch>
      );
    }

    case "LOGGED_OUT":
      return (
        <Switch>
          <Route path="/login">
            <LoginScreen />
          </Route>

          <Route path="/signup">
            <SignupScreen />
          </Route>
          <Route path="/forgot-password" component={ForgotPasswordScreen} />
          <Route path="/reset-password" component={ResetPasswordScreen} />
          <Route path="/verify-email" component={VerifyEmailScreen} />
          <Route>
            <Redirect href="/login" />
          </Route>
        </Switch>
      );

    default:
      return tokenState satisfies never;
  }
};
