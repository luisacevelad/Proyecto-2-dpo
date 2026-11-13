package cds.consola;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import cds.ReglaNegocioException;
import cds.academias.Academia;
import cds.academias.InscripcionAcademia;
import cds.academias.Instalacion;
import cds.academias.Modalidad;
import cds.academias.PartidoPractica;
import cds.academias.RegistroRanking;
import cds.academias.Reserva;
import cds.conjunto.Equipo;
import cds.entrenamiento.Asistencia;
import cds.entrenamiento.SesionEntrenamiento;
import cds.partidos.Partido;
import cds.usuarios.RestriccionMedica;
import cds.usuarios.Socio;
import cds.usuarios.Usuario;
import cds.ventas.Mensualidad;

/** Consola para los socios del club. */
public class ConsolaSocio extends ConsolaBase {

	public static void main(String[] args) {
		new ConsolaSocio().iniciar();
	}

	@Override
	protected boolean aceptaUsuario(Usuario u) {
		return u instanceof Socio;
	}

	@Override
	protected String nombreRol() {
		return "Socio";
	}

	private Socio socio() {
		return (Socio) usuario;
	}

	@Override
	protected Menu construirMenu() {
		Menu academias = new Menu("Academias", entrada)
				.agregar("Ver mi ranking y su evolucion", this::verRanking)
				.agregar("Reservar una instalacion", this::reservar)
				.agregar("Ver mis reservas", this::verReservas)
				.agregar("Programar partido de practica", this::programarPractica)
				.agregar("Registrar resultado de un partido de practica", this::resultadoPractica);

		Menu menu = new Menu("Socio - " + usuario.getNombre(), entrada);
		menu.agregar("Ver mi perfil", this::verPerfil);
		menu.agregar("Ver mis equipos", this::verEquipos);
		menu.agregar("Inscribirme en un equipo", this::inscribirseEnEquipo);
		menu.agregarSubmenu("Academias, reservas y practicas", academias);
		menu.agregar("Ver mis entrenamientos y evaluaciones", this::verEntrenamientos);
		menu.agregarSubmenu("Tienda y cafeteria", new MenuCompras(this).construir());
		menu.agregar("Ver y pagar mensualidades", this::mensualidades);
		menu.agregarSubmenu("Torneos", new MenuTorneosJugador(this).construir());
		return menu;
	}

	private void verPerfil() {
		Socio s = socio();
		System.out.println("Nombre: " + s.getNombre() + " | Documento: " + s.getDocumento());
		System.out.println("Edad: " + s.getEdad(hoy()) + " | Activo: " + (s.isActivo() ? "si" : "no"));
		System.out.println("Habilitado para jugar: " + (s.estaHabilitado() ? "si" : "no"));
		for (RestriccionMedica r : s.getRestricciones()) {
			if (r.estaVigente()) {
				System.out.println("  Restriccion medica desde " + r.getFechaInicio() + ": " + r.getMotivo());
			}
		}
		System.out.println("Puntos de fidelidad: " + s.getPuntosFidelidad());
	}

	private void verEquipos() {
		boolean alguno = false;
		for (Equipo e : club.getEquipos()) {
			if (e.tieneJugador(socio())) {
				alguno = true;
				System.out.println("- " + e.getNombre() + " (" + e.getDeporte().getNombre() + " "
						+ e.getCategoria().getNombre() + ")" + (e.estaSuspendido(socio()) ? " SUSPENDIDO" : ""));
			}
		}
		if (!alguno) {
			System.out.println("No pertenece a ningun equipo.");
		}
	}

	/** HU-SOC-02: el socio elige equipo, dorsal y posicion; el modelo valida cupo, edad, dorsal y deporte. */
	private void inscribirseEnEquipo() throws ReglaNegocioException {
		List<Equipo> activos = new ArrayList<Equipo>();
		for (Equipo e : club.getEquipos()) {
			if (e.isActivo()) {
				activos.add(e);
			}
		}
		Equipo equipo = elegirEquipo(activos);
		String posicion = entrada.texto("Posicion");
		int dorsal = entrada.entero("Numero de camiseta", 1, 99);
		club.inscribirJugador(equipo, socio(), posicion, dorsal, hoy());
		System.out.println("Inscrito en " + equipo.getNombre() + " con el dorsal " + dorsal + ".");
	}

	private List<Academia> misAcademias() {
		List<Academia> resultado = new ArrayList<Academia>();
		for (Academia a : club.getAcademias()) {
			if (a.getInscripcion(socio()) != null) {
				resultado.add(a);
			}
		}
		return resultado;
	}

	private void verRanking() throws ReglaNegocioException {
		Academia a = elegirAcademia(misAcademias());
		InscripcionAcademia i = a.getInscripcion(socio());
		System.out.println("Nivel: " + i.getNivel() + " | Puntaje actual: " + Math.round(i.getPuntajeRanking()));
		List<RegistroRanking> historial = club.evolucionRanking(a, socio());
		if (historial.isEmpty()) {
			System.out.println("Todavia no tiene partidos de practica registrados.");
		}
		for (RegistroRanking r : historial) {
			System.out.println("  " + r.getFecha() + ": " + Math.round(r.getPuntaje()));
		}
	}

	private void reservar() throws ReglaNegocioException {
		Academia a = elegirAcademia(misAcademias());
		Instalacion inst = elegirInstalacion(a.getInstalaciones());
		LocalDate fecha = entrada.fechaOHoy("Fecha");
		LocalTime inicio = entrada.hora("Hora de inicio");
		LocalTime fin = entrada.hora("Hora de fin");
		Modalidad modalidad = entrada.elegirEnum("Modalidad", Modalidad.values());
		int jugadores = entrada.entero("Numero de jugadores", 1, 4);
		club.reservar(a, inst, socio(), fecha, inicio, fin, jugadores, modalidad, LocalDateTime.now());
		System.out.println("Reserva registrada: " + inst.getNombre() + " el " + fecha + " de " + inicio + " a " + fin);
	}

	private void verReservas() {
		boolean alguna = false;
		for (Academia a : club.getAcademias()) {
			for (Reserva r : a.getReservas()) {
				if (r.getSocio() == socio()) {
					alguna = true;
					System.out.println("- " + r.getFecha() + " " + r.getHoraInicio() + "-" + r.getHoraFin() + " "
							+ r.getInstalacion().getNombre() + " (" + r.getModalidad() + ", " + r.getNumJugadores() + " jugadores)");
				}
			}
		}
		if (!alguna) {
			System.out.println("No tiene reservas.");
		}
	}

	private void programarPractica() throws ReglaNegocioException {
		Academia a = elegirAcademia(misAcademias());
		Modalidad modalidad = entrada.elegirEnum("Modalidad", Modalidad.values());
		int jugadores = modalidad == Modalidad.DOBLES ? 4 : 2;
		LocalDate fecha = entrada.fechaOHoy("Fecha del partido");
		PartidoPractica partido = new PartidoPractica(fecha, a, modalidad);
		partido.agregarParticipante(socio());
		List<Socio> candidatos = new ArrayList<Socio>();
		for (InscripcionAcademia i : a.getInscripciones()) {
			candidatos.add(i.getSocio());
		}
		if (modalidad == Modalidad.DOBLES) {
			System.out.println("Orden: jugadores 1 y 2 forman un lado; 3 y 4 el otro.");
		}
		new RegistroPartido(entrada).agregarParticipantes(partido, candidatos, jugadores);
		if (partido.getParticipantes().size() != jugadores) {
			throw new ReglaNegocioException("El partido de " + modalidad + " necesita " + jugadores + " jugadores; no se programo");
		}
		club.registrarPartido(partido);
		System.out.println("Partido de practica programado para el " + fecha + ".");
	}

	private void resultadoPractica() throws ReglaNegocioException {
		List<PartidoPractica> pendientes = new ArrayList<PartidoPractica>();
		for (Partido p : club.getPartidos()) {
			if (p instanceof PartidoPractica && !p.isJugado() && p.getParticipantes().contains(socio())) {
				pendientes.add((PartidoPractica) p);
			}
		}
		PartidoPractica p = entrada.elegir("Partidos pendientes", pendientes,
				x -> x.getFecha() + " " + x.getAcademia().getNombre() + " " + x.getModalidad() + ": " + lado(x, 0) + " vs " + lado(x, 1));
		RegistroPartido registro = new RegistroPartido(entrada);
		registro.estadisticasIndividuales(p, p.getAcademia().getDisciplina());
		registro.finalizar(p, lado(p, 0), lado(p, 1));
		System.out.println("Rankings actualizados. Su puntaje: " + Math.round(p.getAcademia().getInscripcion(socio()).getPuntajeRanking()));
	}

	/** Nombres de un lado del partido (0 = local, 1 = visitante). */
	private String lado(PartidoPractica p, int lado) {
		int porLado = p.getModalidad() == Modalidad.DOBLES ? 2 : 1;
		List<Socio> jugadores = p.getParticipantes();
		StringBuilder sb = new StringBuilder();
		for (int i = lado * porLado; i < (lado + 1) * porLado && i < jugadores.size(); i++) {
			if (sb.length() > 0) {
				sb.append(" y ");
			}
			sb.append(jugadores.get(i).getNombre());
		}
		return sb.toString();
	}

	private void verEntrenamientos() {
		boolean alguno = false;
		for (SesionEntrenamiento s : club.getSesiones()) {
			for (Asistencia a : s.getConvocados()) {
				if (a.getSocio() == socio()) {
					alguno = true;
					String nota = a.getEvaluacion() == null ? "sin evaluar"
							: "nota " + a.getEvaluacion().getNota() + " - " + a.getEvaluacion().getComentario();
					System.out.println("- " + s.getFecha() + " " + s.getHora() + " con " + s.getResponsable().getNombre()
							+ ": " + a.getEstado() + ", " + nota);
				}
			}
		}
		if (!alguno) {
			System.out.println("No ha sido convocado a entrenamientos.");
			return;
		}
		System.out.println("Asistio a " + club.asistencias(socio()) + " de " + club.convocatorias(socio())
				+ " sesiones finalizadas.");
	}

	private void mensualidades() throws ReglaNegocioException {
		List<Mensualidad> pendientes = new ArrayList<Mensualidad>();
		for (Mensualidad m : club.getTienda().getMensualidades()) {
			if (m.getSocio() == socio()) {
				System.out.println("- " + m.getMes() + ": " + pesos(m.getMonto())
						+ (m.isPagada() ? " (pagada el " + m.getFechaPago() + ")" : " (PENDIENTE)"));
				if (!m.isPagada()) {
					pendientes.add(m);
				}
			}
		}
		if (pendientes.isEmpty()) {
			System.out.println("No tiene mensualidades pendientes.");
			return;
		}
		if (entrada.siNo("Desea pagar una mensualidad?")) {
			Mensualidad m = entrada.elegir("Mensualidades pendientes", pendientes, x -> x.getMes() + " - " + pesos(x.getMonto()));
			m.pagar(hoy());
			System.out.println("Mensualidad de " + m.getMes() + " pagada.");
		}
	}
}
