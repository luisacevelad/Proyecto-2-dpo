package cds.consola;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cds.ReglaNegocioException;
import cds.academias.Academia;
import cds.academias.Instalacion;
import cds.competencias.Competencia;
import cds.competencias.CompetenciaPorFase;
import cds.competencias.Fase;
import cds.competencias.Liga;
import cds.competencias.NominaCompetencia;
import cds.competencias.PartidoCompetencia;
import cds.conjunto.Equipo;
import cds.conjunto.MiembroEquipo;
import cds.conjunto.PartidoInterno;
import cds.entrenamiento.Asistencia;
import cds.entrenamiento.EstadoAsistencia;
import cds.entrenamiento.SesionEntrenamiento;
import cds.partidos.Partido;
import cds.usuarios.Entrenador;
import cds.usuarios.Socio;

/** Opciones propias del entrenador (HU-ENT-01 a HU-ENT-04). */
public class MenuEntrenador {
	private final ConsolaEmpleado consola;
	private final Entrada entrada;

	public MenuEntrenador(ConsolaEmpleado consola) {
		this.consola = consola;
		this.entrada = consola.entrada;
	}

	public Menu construir() {
		Menu sesiones = new Menu("Sesiones de entrenamiento", entrada)
				.agregar("Crear sesion", this::crearSesion)
				.agregar("Convocar jugadores a una sesion", this::convocar)
				.agregar("Finalizar una sesion", this::finalizarSesion)
				.agregar("Registrar asistencia y evaluacion", this::registrarAsistencia);
		Menu partidos = new Menu("Partidos y competencias", entrada)
				.agregar("Registrar partido interno", this::partidoInterno)
				.agregar("Completar un partido interno pendiente", this::completarInternoPendiente)
				.agregar("Verificar si un equipo alcanza el minimo de habilitados", this::verificarMinimo)
				.agregar("Inscribir nomina de un equipo en una competencia", this::inscribirNomina)
				.agregar("Registrar partido oficial (alineacion y resultado)", this::partidoOficial)
				.agregar("Completar un partido oficial pendiente", this::completarOficialPendiente)
				.agregar("Ver tabla de una liga", this::verTabla);
		return new Menu("Entrenador", entrada)
				.agregarSubmenu("Sesiones de entrenamiento", sesiones)
				.agregar("Inscribir jugador en un equipo", this::inscribirJugador)
				.agregarSubmenu("Partidos y competencias", partidos);
	}

	private Entrenador entrenador() {
		return (Entrenador) consola.usuario;
	}

	// ---------------------------- Sesiones ----------------------------

	private void crearSesion() throws ReglaNegocioException {
		LocalDate fecha = entrada.fechaOHoy("Fecha");
		LocalTime hora = entrada.hora("Hora");
		int duracion = entrada.entero("Duracion en minutos", 30, 240);
		Equipo equipo = null;
		Academia academia = null;
		Instalacion instalacion = null;
		if (entrada.siNo("La sesion es de un equipo? (n = de una academia)")) {
			equipo = consola.elegirEquipo(consola.club.getEquipos());
		} else {
			academia = consola.elegirAcademia(consola.club.getAcademias());
			if (!academia.getInstalaciones().isEmpty() && entrada.siNo("Asignar una instalacion?")) {
				instalacion = consola.elegirInstalacion(academia.getInstalaciones());
			}
		}
		consola.club.registrarSesion(new SesionEntrenamiento(fecha, hora, duracion, entrenador(), instalacion, equipo, academia));
		System.out.println("Sesion creada para el " + fecha + " a las " + hora + ".");
	}

	private List<SesionEntrenamiento> misSesiones(boolean finalizadas) {
		List<SesionEntrenamiento> resultado = new ArrayList<SesionEntrenamiento>();
		for (SesionEntrenamiento s : consola.club.getSesiones()) {
			if (s.getResponsable() == entrenador() && s.isFinalizado() == finalizadas) {
				resultado.add(s);
			}
		}
		return resultado;
	}

	private static String describir(SesionEntrenamiento s) {
		String de = s.getEquipo() != null ? s.getEquipo().getNombre() : s.getAcademia().getNombre();
		return s.getFecha() + " " + s.getHora() + " - " + de + " (" + s.getConvocados().size() + " convocados)";
	}

	private void convocar() throws ReglaNegocioException {
		SesionEntrenamiento sesion = entrada.elegir("Sesiones abiertas", misSesiones(false), MenuEntrenador::describir);
		List<Socio> candidatos = new ArrayList<Socio>();
		if (sesion.getEquipo() != null) {
			for (MiembroEquipo m : sesion.getEquipo().getMiembros()) {
				candidatos.add(m.getSocio());
			}
		} else {
			candidatos.addAll(consola.socios());
		}
		do {
			Socio s = consola.elegirSocio(candidatos);
			try {
				sesion.convocar(s);
				System.out.println("  " + s.getNombre() + " convocado.");
			} catch (ReglaNegocioException e) {
				System.out.println("  Rechazado: " + e.getMessage());
			}
		} while (entrada.siNo("Convocar a otro jugador?"));
	}

	private void finalizarSesion() throws ReglaNegocioException {
		SesionEntrenamiento sesion = entrada.elegir("Sesiones abiertas", misSesiones(false), MenuEntrenador::describir);
		sesion.finalizar();
		System.out.println("Sesion finalizada. Ya puede registrar la asistencia.");
	}

	private void registrarAsistencia() throws ReglaNegocioException {
		SesionEntrenamiento sesion = entrada.elegir("Sesiones finalizadas", misSesiones(true), MenuEntrenador::describir);
		for (Asistencia a : sesion.getConvocados()) {
			System.out.println("Jugador: " + a.getSocio().getNombre() + " (estado actual: " + a.getEstado() + ")");
			EstadoAsistencia estado = entrada.elegirEnum("Asistencia", EstadoAsistencia.values());
			a.registrarEstado(estado);
			if (estado == EstadoAsistencia.ASISTIO || estado == EstadoAsistencia.LLEGO_TARDE) {
				int nota = entrada.entero("Calificacion", 1, 5);
				a.calificar(nota, entrada.texto("Comentario"));
			}
		}
		System.out.println("Asistencia registrada.");
	}

	// ---------------------------- Equipos ----------------------------

	private void inscribirJugador() throws ReglaNegocioException {
		Equipo equipo = consola.elegirEquipo(consola.club.getEquipos());
		Socio s = consola.elegirSocio(consola.socios());
		String posicion = entrada.texto("Posicion");
		int numero = entrada.entero("Numero de camiseta", 1, 99);
		consola.club.inscribirJugador(equipo, s, posicion, numero, consola.hoy());
		System.out.println(s.getNombre() + " inscrito en " + equipo.getNombre() + ".");
	}

	private static List<Socio> jugadoresDe(Equipo e) {
		List<Socio> resultado = new ArrayList<Socio>();
		for (MiembroEquipo m : e.getMiembros()) {
			resultado.add(m.getSocio());
		}
		return resultado;
	}

	// ---------------------------- Partidos internos ----------------------------

	private void partidoInterno() throws ReglaNegocioException {
		System.out.println("Equipo local");
		Equipo local = consola.elegirEquipo(consola.club.getEquipos());
		System.out.println("Equipo visitante");
		Equipo visitante = consola.elegirEquipo(consola.club.getEquipos());
		LocalDate fecha = entrada.fechaOHoy("Fecha del partido");
		PartidoInterno partido = consola.club.registrarPartidoInterno(local, visitante, fecha);
		System.out.println("Partido registrado. Ahora indique la alineacion y el resultado.");
		completarInterno(partido);
	}

	private void completarInternoPendiente() throws ReglaNegocioException {
		List<PartidoInterno> pendientes = new ArrayList<PartidoInterno>();
		for (Partido p : consola.club.getPartidos()) {
			if (p instanceof PartidoInterno && !p.isJugado()) {
				pendientes.add((PartidoInterno) p);
			}
		}
		completarInterno(entrada.elegir("Partidos internos pendientes", pendientes,
				p -> p.getFecha() + ": " + p.getLocal().getNombre() + " vs " + p.getVisitante().getNombre()));
	}

	private void completarInterno(PartidoInterno partido) throws ReglaNegocioException {
		List<Socio> candidatos = jugadoresDe(partido.getLocal());
		candidatos.addAll(jugadoresDe(partido.getVisitante()));
		RegistroPartido registro = new RegistroPartido(entrada);
		registro.agregarParticipantes(partido, candidatos, candidatos.size());
		registro.estadisticasConjunto(partido, partido.getLocal().getDeporte());
		registro.finalizar(partido, partido.getLocal().getNombre(), partido.getVisitante().getNombre());
	}

	/** HU-ENT-02: confirma si el equipo puede jugar y lista a los no disponibles con su motivo. */
	private void verificarMinimo() throws ReglaNegocioException {
		Equipo equipo = consola.elegirEquipo(consola.club.getEquipos());
		int minimo = equipo.getDeporte().getMinJugadores();
		System.out.println(equipo.getNombre() + ": " + equipo.contarHabilitados() + " jugadores habilitados (minimo " + minimo + ").");
		if (equipo.puedeDisputarPartido()) {
			System.out.println("El partido SI puede disputarse.");
		} else {
			System.out.println("El partido NO puede jugarse.");
		}
		Map<Socio, String> noDisponibles = equipo.jugadoresNoDisponibles();
		if (!noDisponibles.isEmpty()) {
			System.out.println("Jugadores no disponibles:");
			for (Map.Entry<Socio, String> e : noDisponibles.entrySet()) {
				System.out.println("  - " + e.getKey().getNombre() + ": " + e.getValue());
			}
		}
	}

	// ---------------------------- Competencias ----------------------------

	private void inscribirNomina() throws ReglaNegocioException {
		Competencia competencia = consola.elegirCompetencia();
		Equipo equipo = consola.elegirEquipo(consola.club.getEquipos());
		List<Socio> nomina = new ArrayList<Socio>();
		for (Socio s : jugadoresDe(equipo)) {
			if (!equipo.puedeAlinear(s)) {
				System.out.println("  " + s.getNombre() + " no se incluye: esta inhabilitado o suspendido.");
			} else if (entrada.siNo("Incluir a " + s.getNombre() + "?")) {
				nomina.add(s);
			}
		}
		competencia.inscribirEquipo(equipo, consola.hoy(), nomina);
		System.out.println("Nomina de " + equipo.getNombre() + " inscrita en " + competencia.getNombre()
				+ " con " + nomina.size() + " jugadores.");
	}

	private void partidoOficial() throws ReglaNegocioException {
		Competencia competencia = consola.elegirCompetencia();
		NominaCompetencia nomina = entrada.elegir("Equipos inscritos", competencia.getNominas(),
				n -> n.getEquipo().getNombre() + " (" + n.getInscritos().size() + " en nomina)");
		LocalDate fecha = entrada.fechaOHoy("Fecha del partido");
		String rival = entrada.texto("Institucion rival");
		String escenario = entrada.texto("Escenario");
		Fase fase = null;
		if (competencia instanceof CompetenciaPorFase && !((CompetenciaPorFase) competencia).getFases().isEmpty()) {
			fase = entrada.elegir("Fases", ((CompetenciaPorFase) competencia).getFases(), f -> f.getNombre() + " (" + f.getTipo() + ")");
		}
		PartidoCompetencia partido = new PartidoCompetencia(fecha, nomina, rival, escenario, competencia.getDeporte());
		if (fase != null) {
			partido.asignarFase(fase);
		}
		System.out.println("Partido creado. Ahora defina la alineacion y el resultado.");
		completarOficial(partido);
	}

	private void completarOficialPendiente() throws ReglaNegocioException {
		List<PartidoCompetencia> pendientes = new ArrayList<PartidoCompetencia>();
		for (Competencia c : consola.club.getCompetencias()) {
			for (NominaCompetencia n : c.getNominas()) {
				for (PartidoCompetencia p : n.getPartidos()) {
					if (!p.isJugado()) {
						pendientes.add(p);
					}
				}
			}
		}
		completarOficial(entrada.elegir("Partidos oficiales pendientes", pendientes,
				p -> p.getFecha() + ": " + p.getNomina().getEquipo().getNombre() + " vs " + p.getInstitucionRival()));
	}

	private void completarOficial(PartidoCompetencia partido) throws ReglaNegocioException {
		Equipo equipo = partido.getNomina().getEquipo();
		RegistroPartido registro = new RegistroPartido(entrada);
		System.out.println("Alineacion (solo jugadores de la nomina y sin suspension):");
		List<Socio> alineacion = registro.elegirVarios(jugadoresDe(equipo));
		partido.confirmarAlineacion(alineacion);
		System.out.println("Alineacion registrada con " + alineacion.size() + " jugadores.");
		registro.estadisticasConjunto(partido, partido.getDeporte());
		registro.finalizar(partido, equipo.getNombre(), partido.getInstitucionRival());
		// PartidoCompetencia no tiene asignada su competencia, asi que la tabla se actualiza aqui
		if (partido.getComeptencia() == null) {
			partido.getNomina().getCompetencia().registrarResultado(partido);
		}
	}

	private void verTabla() throws ReglaNegocioException {
		List<Liga> ligas = new ArrayList<Liga>();
		for (Competencia c : consola.club.getCompetencias()) {
			if (c instanceof Liga) {
				ligas.add((Liga) c);
			}
		}
		Liga liga = entrada.elegir("Ligas", ligas, Liga::getNombre);
		List<Equipo> equipos = new ArrayList<Equipo>(liga.getEquiposTabla());
		if (equipos.isEmpty()) {
			System.out.println("La liga aun no tiene resultados.");
			return;
		}
		equipos.sort((a, b) -> liga.getPuntos(b) - liga.getPuntos(a));
		for (int i = 0; i < equipos.size(); i++) {
			System.out.println((i + 1) + ". " + equipos.get(i).getNombre() + " - " + liga.getPuntos(equipos.get(i)) + " pts");
		}
	}
}
