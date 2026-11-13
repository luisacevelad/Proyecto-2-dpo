package cds.consola;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import cds.ReglaNegocioException;
import cds.usuarios.Empleado;
import cds.usuarios.Entrenador;
import cds.usuarios.Estudiante;
import cds.usuarios.Externo;
import cds.usuarios.EstadoSolicitud;
import cds.usuarios.Fisioterapeuta;
import cds.usuarios.SolicitudCambioTurno;
import cds.usuarios.Socio;
import cds.usuarios.Turno;
import cds.usuarios.Usuario;

/** Administracion de usuarios y turnos (incluye HU-ADM-01). */
public class MenuAdminPersonal {
	private final ConsolaAdministrador consola;
	private final Entrada entrada;

	public MenuAdminPersonal(ConsolaAdministrador consola) {
		this.consola = consola;
		this.entrada = consola.entrada;
	}

	public Menu construir() {
		return new Menu("Usuarios y turnos", entrada)
				.agregar("Registrar socio", this::registrarSocio)
				.agregar("Registrar empleado (entrenador o fisioterapeuta)", this::registrarEmpleado)
				.agregar("Registrar estudiante de la universidad o persona externa (para torneos)", this::registrarParticipante)
				.agregar("Ver usuarios", this::verUsuarios)
				.agregar("Dar de baja a un socio", this::darDeBaja)
				.agregar("Asignar turno a un empleado", this::asignarTurno)
				.agregar("Ver turnos", this::verTurnos)
				.agregar("Resolver solicitudes de cambio de turno", this::resolverSolicitudes);
	}

	private String loginNuevo() {
		while (true) {
			String login = entrada.texto("Login");
			if (login.contains(" ")) {
				System.out.println("  El login no puede tener espacios.");
			} else if (consola.club.buscarUsuario(login) != null) {
				System.out.println("  Ya existe un usuario con ese login.");
			} else {
				return login;
			}
		}
	}

	private void registrarSocio() throws ReglaNegocioException {
		String login = loginNuevo();
		String password = entrada.texto("Password");
		String nombre = entrada.texto("Nombre completo");
		String documento = entrada.texto("Documento");
		LocalDate nacimiento = entrada.fecha("Fecha de nacimiento");
		if (!nacimiento.isBefore(consola.hoy())) {
			throw new ReglaNegocioException("La fecha de nacimiento debe ser anterior a hoy");
		}
		Socio socio = new Socio(login, password, nombre, documento, nacimiento);
		if (entrada.siNo("Este socio tambien es entrenador?")) {
			socio.marcarComoEntrenador();
		}
		consola.club.registrarUsuario(socio);
		System.out.println("Socio " + nombre + " registrado.");
	}

	private void registrarEmpleado() throws ReglaNegocioException {
		boolean esEntrenador = entrada.siNo("Es entrenador? (n = fisioterapeuta)");
		String login = loginNuevo();
		String password = entrada.texto("Password");
		String nombre = entrada.texto("Nombre completo");
		String codigo = entrada.texto("Codigo de descuento");
		Empleado empleado = esEntrenador ? new Entrenador(login, password, nombre, codigo)
				: new Fisioterapeuta(login, password, nombre, codigo);
		consola.club.registrarUsuario(empleado);
		System.out.println("Empleado " + nombre + " registrado. Recuerde asignarle turnos.");
	}

	/** Estudiantes y externos solo participan en torneos; entran por la consola de socio / jugador. */
	private void registrarParticipante() throws ReglaNegocioException {
		boolean estudiante = entrada.siNo("Es estudiante de la universidad? (n = persona externa)");
		String login = loginNuevo();
		String password = entrada.texto("Password");
		String nombre = entrada.texto("Nombre completo");
		Usuario nuevo = estudiante ? new Estudiante(login, password, nombre, entrada.texto("Codigo estudiantil"))
				: new Externo(login, password, nombre, entrada.texto("Documento"));
		consola.club.registrarUsuario(nuevo);
		System.out.println((estudiante ? "Estudiante " : "Externo ") + nombre
				+ " registrado. Ya puede entrar a la consola de socio / jugador para inscribirse en torneos.");
	}

	private void verUsuarios() {
		for (Usuario u : consola.club.getUsuarios()) {
			String extra = "";
			if (u instanceof Socio) {
				extra = ((Socio) u).isActivo() ? "" : " (de baja)";
			}
			System.out.println("- " + u.getClass().getSimpleName() + ": " + u.getNombre() + " [" + u.getLogin() + "]" + extra);
		}
	}

	private void darDeBaja() throws ReglaNegocioException {
		List<Socio> activos = new ArrayList<Socio>();
		for (Socio s : consola.socios()) {
			if (s.isActivo()) {
				activos.add(s);
			}
		}
		Socio s = consola.elegirSocio(activos);
		if (entrada.siNo("Confirma dar de baja a " + s.getNombre() + "?")) {
			s.darDeBaja();
			System.out.println(s.getNombre() + " fue dado de baja.");
		}
	}

	private void asignarTurno() throws ReglaNegocioException {
		Empleado e = consola.entrada.elegir("Empleados", consola.empleados(),
				x -> x.getNombre() + " (" + x.getClass().getSimpleName() + ")");
		DayOfWeek dia = entrada.elegirEnum("Dia", DayOfWeek.values());
		LocalTime inicio = entrada.hora("Hora de inicio");
		LocalTime fin = entrada.hora("Hora de fin");
		consola.club.asignarTurno(e, dia, inicio, fin);
		System.out.println("Turno asignado a " + e.getNombre() + ".");
	}

	private void verTurnos() {
		if (consola.club.getTurnos().isEmpty()) {
			System.out.println("No hay turnos registrados.");
		}
		for (Turno t : consola.club.getTurnos()) {
			System.out.println("- " + t.getEmpleado().getNombre() + ": " + t.getDia() + " " + t.getHoraInicio() + "-" + t.getHoraFin());
		}
	}

	private void resolverSolicitudes() throws ReglaNegocioException {
		List<SolicitudCambioTurno> pendientes = new ArrayList<SolicitudCambioTurno>();
		for (SolicitudCambioTurno s : consola.club.getSolicitudes()) {
			if (s.getEstado() == EstadoSolicitud.PENDIENTE) {
				pendientes.add(s);
			}
		}
		SolicitudCambioTurno s = entrada.elegir("Solicitudes pendientes", pendientes,
				x -> x.getSolicitante().getNombre() + ": " + x.getTurno().getDia() + " " + x.getTurno().getHoraInicio() + "-"
						+ x.getTurno().getHoraFin() + " -> " + x.getNuevoDia() + " " + x.getNuevaHoraInicio() + "-" + x.getNuevaHoraFin());
		boolean aprobar = entrada.siNo("Aprobar la solicitud? (n = rechazar)");
		consola.club.resolverSolicitud(consola.administrador(), s, aprobar);
		System.out.println("Solicitud " + s.getEstado() + ".");
	}
}
