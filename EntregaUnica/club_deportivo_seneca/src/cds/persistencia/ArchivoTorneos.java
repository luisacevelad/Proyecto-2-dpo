package cds.persistencia;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import cds.Club;
import cds.academias.Disciplina;
import cds.academias.Nivel;
import cds.conjunto.CategoriaEdad;
import cds.conjunto.DeporteConjunto;
import cds.conjunto.Equipo;
import cds.torneos.EncuentroTorneo;
import cds.torneos.EquipoTorneo;
import cds.torneos.Especialidad;
import cds.torneos.EspecialidadConjunto;
import cds.torneos.EspecialidadIndividual;
import cds.torneos.EstadoTorneo;
import cds.torneos.InscripcionTorneo;
import cds.torneos.JugadorTorneo;
import cds.torneos.ParejaTorneo;
import cds.torneos.Participante;
import cds.torneos.Torneo;
import cds.torneos.TorneoCompetitivo;
import cds.torneos.TorneoRecreativo;
import cds.usuarios.Usuario;
import cds.ReglaNegocioException;

/**
 * Guarda y carga los torneos en la seccion "torneos" de club.json.
 *
 * Se separa de ArchivoClub para que cada clase tenga una sola responsabilidad.
 * Los torneos se reconstruyen con los metodos restaurar* de Torneo y
 * EncuentroTorneo, que no vuelven a validar reglas (por ejemplo, fechas de
 * inscripcion ya pasadas).
 *
 * Referencias que usa el archivo:
 * - Usuarios por login y equipos por nombre (ya cargados por ArchivoClub).
 * - Inscripciones de un encuentro y el ganador por su posicion en la lista de
 *   inscripciones del mismo torneo (-1 = sin ganador).
 */
public final class ArchivoTorneos {
	static final String SECCION = "torneos";

	private static final String RECREATIVO = "RECREATIVO";
	private static final String COMPETITIVO = "COMPETITIVO";
	private static final String CONJUNTO = "CONJUNTO";
	private static final String INDIVIDUAL = "INDIVIDUAL";
	private static final String JUGADOR = "JUGADOR";
	private static final String PAREJA = "PAREJA";
	private static final String EQUIPO = "EQUIPO";

	private ArchivoTorneos() {
	}

	// ----------------------------------------------------------------------
	// Guardar
	// ----------------------------------------------------------------------

	static List<Object> guardar(Club club) throws IOException {
		List<Object> resultado = Json.arreglo();
		for (Torneo torneo : club.getTorneos()) {
			resultado.add(guardarTorneo(torneo));
		}
		return resultado;
	}

	private static Map<String, Object> guardarTorneo(Torneo torneo) throws IOException {
		Map<String, Object> item = Json.objeto();
		if (torneo instanceof TorneoRecreativo) {
			item.put("tipo", RECREATIVO);
			item.put("porcentajeBono", Double.valueOf(((TorneoRecreativo) torneo).getPorcentajeBono()));
		} else if (torneo instanceof TorneoCompetitivo) {
			item.put("tipo", COMPETITIVO);
			item.put("tarifa", Double.valueOf(((TorneoCompetitivo) torneo).getTarifa()));
		} else {
			throw new IOException("Tipo de torneo no soportado al guardar: " + torneo.getClass().getSimpleName());
		}
		item.put("nombre", torneo.getNombre());
		item.put("especialidad", guardarEspecialidad(torneo.getEspecialidad()));
		item.put("cupos", Integer.valueOf(torneo.getCupos()));
		item.put("fechaInicio", torneo.getFechaInicio().toString());
		item.put("fechaLimitePreferencial", torneo.getFechaLimitePreferencial().toString());
		item.put("estado", torneo.getEstado().name());
		item.put("preferencialesLiberados", Boolean.valueOf(torneo.isPreferencialesLiberados()));

		List<InscripcionTorneo> inscripciones = torneo.getInscripciones();
		List<Object> datosInscripciones = Json.arreglo();
		for (InscripcionTorneo inscripcion : inscripciones) {
			Map<String, Object> dato = Json.objeto();
			dato.put("participante", guardarParticipante(inscripcion.getParticipante()));
			dato.put("fecha", inscripcion.getFecha().toString());
			dato.put("preferencial", Boolean.valueOf(inscripcion.isPreferencial()));
			dato.put("montoPagado", Double.valueOf(inscripcion.getMontoPagado()));
			datosInscripciones.add(dato);
		}
		item.put("inscripciones", datosInscripciones);

		List<Object> datosEncuentros = Json.arreglo();
		for (EncuentroTorneo encuentro : torneo.getEncuentros()) {
			Map<String, Object> dato = Json.objeto();
			dato.put("participanteA", Integer.valueOf(inscripciones.indexOf(encuentro.getParticipanteA())));
			dato.put("participanteB", Integer.valueOf(inscripciones.indexOf(encuentro.getParticipanteB())));
			dato.put("fecha", encuentro.getFecha().toString());
			dato.put("ronda", encuentro.getRonda());
			dato.put("jugado", Boolean.valueOf(encuentro.isJugado()));
			dato.put("marcadorA", Integer.valueOf(encuentro.getMarcadorA()));
			dato.put("marcadorB", Integer.valueOf(encuentro.getMarcadorB()));
			datosEncuentros.add(dato);
		}
		item.put("encuentros", datosEncuentros);
		item.put("ganador", Integer.valueOf(inscripciones.indexOf(torneo.getGanador())));
		return item;
	}

	private static Map<String, Object> guardarEspecialidad(Especialidad especialidad) throws IOException {
		Map<String, Object> item = Json.objeto();
		if (especialidad instanceof EspecialidadConjunto) {
			EspecialidadConjunto c = (EspecialidadConjunto) especialidad;
			item.put("tipo", CONJUNTO);
			item.put("deporte", c.getDeporte().name());
			item.put("categoria", c.getCategoria().name());
		} else if (especialidad instanceof EspecialidadIndividual) {
			EspecialidadIndividual i = (EspecialidadIndividual) especialidad;
			item.put("tipo", INDIVIDUAL);
			item.put("disciplina", i.getDisciplina().name());
			item.put("nivel", i.getNivel().name());
			item.put("dobles", Boolean.valueOf(i.isDobles()));
		} else {
			throw new IOException("Especialidad no soportada al guardar: " + especialidad.getClass().getSimpleName());
		}
		return item;
	}

	private static Map<String, Object> guardarParticipante(Participante participante) throws IOException {
		Map<String, Object> item = Json.objeto();
		if (participante instanceof JugadorTorneo) {
			item.put("tipo", JUGADOR);
			item.put("login", ((JugadorTorneo) participante).getJugador().getLogin());
		} else if (participante instanceof ParejaTorneo) {
			ParejaTorneo pareja = (ParejaTorneo) participante;
			item.put("tipo", PAREJA);
			item.put("jugador1", pareja.getJugador1().getLogin());
			item.put("jugador2", pareja.getJugador2().getLogin());
		} else if (participante instanceof EquipoTorneo) {
			item.put("tipo", EQUIPO);
			item.put("equipo", ((EquipoTorneo) participante).getEquipo().getNombre());
		} else {
			throw new IOException("Tipo de participante no soportado al guardar: " + participante.getClass().getSimpleName());
		}
		return item;
	}

	// ----------------------------------------------------------------------
	// Cargar
	// ----------------------------------------------------------------------

	/**
	 * Carga los torneos. Si el archivo es anterior al Proyecto 2 y no tiene la
	 * seccion "torneos", el club queda sin torneos.
	 */
	static void cargar(Club club, Map<String, Object> raiz) throws IOException, ReglaNegocioException {
		if (!raiz.containsKey(SECCION)) {
			return;
		}
		for (Object dato : Json.comoArreglo(raiz.get(SECCION))) {
			club.registrarTorneo(cargarTorneo(club, Json.comoObjeto(dato)));
		}
	}

	private static Torneo cargarTorneo(Club club, Map<String, Object> item) throws IOException, ReglaNegocioException {
		String nombre = Json.texto(item, "nombre");
		Especialidad especialidad = cargarEspecialidad(Json.comoObjeto(item.get("especialidad")));
		int cupos = Json.entero(item, "cupos");
		LocalDate inicio = LocalDate.parse(Json.texto(item, "fechaInicio"));
		LocalDate limite = LocalDate.parse(Json.texto(item, "fechaLimitePreferencial"));
		String tipo = Json.texto(item, "tipo");
		Torneo torneo;
		if (RECREATIVO.equals(tipo)) {
			torneo = new TorneoRecreativo(nombre, especialidad, cupos, inicio, limite, Json.decimal(item, "porcentajeBono"));
		} else if (COMPETITIVO.equals(tipo)) {
			torneo = new TorneoCompetitivo(nombre, especialidad, cupos, inicio, limite, Json.decimal(item, "tarifa"));
		} else {
			throw new IOException("Tipo de torneo desconocido en " + nombre + ": " + tipo);
		}

		for (Object dato : Json.comoArreglo(item.get("inscripciones"))) {
			Map<String, Object> d = Json.comoObjeto(dato);
			Participante participante = cargarParticipante(club, Json.comoObjeto(d.get("participante")));
			torneo.restaurarInscripcion(new InscripcionTorneo(participante, LocalDate.parse(Json.texto(d, "fecha")),
					Json.booleano(d, "preferencial"), Json.decimal(d, "montoPagado")));
		}
		List<InscripcionTorneo> inscripciones = torneo.getInscripciones();

		for (Object dato : Json.comoArreglo(item.get("encuentros"))) {
			Map<String, Object> d = Json.comoObjeto(dato);
			EncuentroTorneo encuentro = new EncuentroTorneo(inscripcion(inscripciones, Json.entero(d, "participanteA"), nombre),
					inscripcion(inscripciones, Json.entero(d, "participanteB"), nombre), LocalDate.parse(Json.texto(d, "fecha")),
					Json.textoOpcional(d, "ronda"));
			if (Json.booleano(d, "jugado")) {
				encuentro.restaurarResultado(Json.entero(d, "marcadorA"), Json.entero(d, "marcadorB"));
			}
			torneo.restaurarEncuentro(encuentro);
		}

		int idGanador = Json.entero(item, "ganador");
		InscripcionTorneo ganador = idGanador < 0 ? null : inscripcion(inscripciones, idGanador, nombre);
		torneo.restaurarEstado(EstadoTorneo.valueOf(Json.texto(item, "estado")), Json.booleano(item, "preferencialesLiberados"),
				ganador);
		return torneo;
	}

	private static Especialidad cargarEspecialidad(Map<String, Object> item) throws IOException {
		String tipo = Json.texto(item, "tipo");
		if (CONJUNTO.equals(tipo)) {
			return new EspecialidadConjunto(DeporteConjunto.valueOf(Json.texto(item, "deporte")),
					CategoriaEdad.valueOf(Json.texto(item, "categoria")));
		}
		if (INDIVIDUAL.equals(tipo)) {
			return new EspecialidadIndividual(Disciplina.valueOf(Json.texto(item, "disciplina")),
					Nivel.valueOf(Json.texto(item, "nivel")), Json.booleano(item, "dobles"));
		}
		throw new IOException("Especialidad de torneo desconocida: " + tipo);
	}

	private static Participante cargarParticipante(Club club, Map<String, Object> item) throws IOException, ReglaNegocioException {
		String tipo = Json.texto(item, "tipo");
		if (JUGADOR.equals(tipo)) {
			return new JugadorTorneo(usuario(club, Json.texto(item, "login")));
		}
		if (PAREJA.equals(tipo)) {
			return new ParejaTorneo(usuario(club, Json.texto(item, "jugador1")), usuario(club, Json.texto(item, "jugador2")));
		}
		if (EQUIPO.equals(tipo)) {
			String nombre = Json.texto(item, "equipo");
			Equipo equipo = club.buscarEquipo(nombre);
			if (equipo == null) {
				throw new IOException("Un torneo menciona un equipo que no existe: " + nombre);
			}
			return new EquipoTorneo(equipo);
		}
		throw new IOException("Tipo de participante desconocido: " + tipo);
	}

	private static Usuario usuario(Club club, String login) throws IOException {
		Usuario usuario = club.buscarUsuario(login);
		if (usuario == null) {
			throw new IOException("Un torneo menciona un usuario que no existe: " + login);
		}
		return usuario;
	}

	private static InscripcionTorneo inscripcion(List<InscripcionTorneo> inscripciones, int id, String torneo) throws IOException {
		if (id < 0 || id >= inscripciones.size()) {
			throw new IOException("Referencia de inscripcion fuera de rango en " + torneo + ": " + id);
		}
		return inscripciones.get(id);
	}
}
