package cds.consola;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import cds.ReglaNegocioException;
import cds.catalogo.Sugerencia;
import cds.catalogo.TipoSugerencia;
import cds.usuarios.Empleado;
import cds.usuarios.Entrenador;
import cds.usuarios.Fisioterapeuta;
import cds.usuarios.SolicitudCambioTurno;
import cds.usuarios.Turno;
import cds.usuarios.Usuario;

/**
 * Consola para los empleados (entrenadores y fisioterapeutas).
 * Las opciones comunes estan aqui; las propias de cada cargo estan en
 * MenuEntrenador y MenuFisioterapeuta.
 */
public class ConsolaEmpleado extends ConsolaBase {

	public static void main(String[] args) {
		new ConsolaEmpleado().iniciar();
	}

	@Override
	protected boolean aceptaUsuario(Usuario u) {
		return u instanceof Empleado;
	}

	@Override
	protected String nombreRol() {
		return "Empleado";
	}

	Empleado empleado() {
		return (Empleado) usuario;
	}

	@Override
	protected Menu construirMenu() {
		Menu turnos = new Menu("Turnos", entrada)
				.agregar("Ver mis turnos", this::verTurnos)
				.agregar("Solicitar cambio de turno", this::solicitarCambio)
				.agregar("Ver mis solicitudes", this::verSolicitudes);

		Menu menu = new Menu("Empleado - " + usuario.getNombre(), entrada);
		if (usuario instanceof Entrenador) {
			menu.agregarSubmenu("Entrenamientos, equipos y partidos", new MenuEntrenador(this).construir());
		}
		if (usuario instanceof Fisioterapeuta) {
			menu.agregarSubmenu("Restricciones medicas", new MenuFisioterapeuta(this).construir());
		}
		menu.agregarSubmenu("Mis turnos", turnos);
		menu.agregar("Sugerir un nuevo articulo o snack", this::enviarSugerencia);
		menu.agregar("Ver el estado de mis sugerencias", this::verSugerencias);
		menu.agregar("Ver mi codigo de descuento", () -> System.out.println("Su codigo: " + empleado().getCodigoDescuento()));
		menu.agregarSubmenu("Tienda y cafeteria", new MenuCompras(this).construir());
		// TODO Torneos (Persona B): inscribirse a torneos individuales si no esta de turno ese dia
		return menu;
	}

	private List<Turno> misTurnos() {
		List<Turno> resultado = new ArrayList<Turno>();
		for (Turno t : club.getTurnos()) {
			if (t.getEmpleado() == empleado()) {
				resultado.add(t);
			}
		}
		return resultado;
	}

	private static String describir(Turno t) {
		return t.getDia() + " " + t.getHoraInicio() + "-" + t.getHoraFin();
	}

	private void verTurnos() {
		List<Turno> turnos = misTurnos();
		if (turnos.isEmpty()) {
			System.out.println("No tiene turnos asignados.");
		}
		for (Turno t : turnos) {
			System.out.println("- " + describir(t));
		}
	}

	private void solicitarCambio() throws ReglaNegocioException {
		Turno turno = entrada.elegir("Mis turnos", misTurnos(), ConsolaEmpleado::describir);
		DayOfWeek dia = entrada.elegirEnum("Nuevo dia", DayOfWeek.values());
		LocalTime inicio = entrada.hora("Nueva hora de inicio");
		LocalTime fin = entrada.hora("Nueva hora de fin");
		club.solicitarCambioTurno(empleado(), turno, dia, inicio, fin);
		System.out.println("Solicitud enviada. El administrador debe aprobarla.");
	}

	private void verSolicitudes() {
		boolean alguna = false;
		for (SolicitudCambioTurno s : club.getSolicitudes()) {
			if (s.getSolicitante() == empleado()) {
				alguna = true;
				System.out.println("- " + describir(s.getTurno()) + " -> " + s.getNuevoDia() + " " + s.getNuevaHoraInicio()
						+ "-" + s.getNuevaHoraFin() + ": " + s.getEstado());
			}
		}
		if (!alguna) {
			System.out.println("No ha enviado solicitudes.");
		}
	}

	private void enviarSugerencia() {
		TipoSugerencia tipo = entrada.elegirEnum("Tipo de producto", TipoSugerencia.values());
		String nombre = entrada.texto("Nombre del producto");
		double precio = entrada.decimal("Precio sugerido", 1, 100000000);
		Sugerencia sugerencia = new Sugerencia(empleado(), nombre, hoy(), tipo, precio);
		if (tipo == TipoSugerencia.SNACK && entrada.siNo("Declarar los alergenos ahora? (sin ellos no se puede aprobar)")) {
			sugerencia.declararAlergenos(MenuAdminTienda.pedirAlergenos(entrada));
		}
		club.getTienda().registrarSugerencia(sugerencia);
		System.out.println("Sugerencia enviada al administrador.");
	}

	private void verSugerencias() {
		boolean alguna = false;
		for (Sugerencia s : club.getTienda().getSugerencias()) {
			if (s.getAutor() == empleado()) {
				alguna = true;
				System.out.println("- " + s.getTipo() + " " + s.getDescripcion() + ": " + s.getEstado());
			}
		}
		if (!alguna) {
			System.out.println("No ha enviado sugerencias.");
		}
	}
}
