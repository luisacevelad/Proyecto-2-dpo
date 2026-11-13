package cds.consola;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import cds.ReglaNegocioException;
import cds.academias.Disciplina;
import cds.academias.Nivel;
import cds.conjunto.CategoriaEdad;
import cds.conjunto.DeporteConjunto;
import cds.torneos.EncuentroTorneo;
import cds.torneos.Especialidad;
import cds.torneos.EspecialidadConjunto;
import cds.torneos.EspecialidadIndividual;
import cds.torneos.EstadoTorneo;
import cds.torneos.FormatoTorneo;
import cds.torneos.InscripcionTorneo;
import cds.torneos.Torneo;
import cds.torneos.TorneoCompetitivo;
import cds.torneos.TorneoRecreativo;

/** Administracion de torneos internos: crear, consultar e historial, y su desarrollo. */
public class MenuAdminTorneos {
	private final ConsolaAdministrador consola;
	private final Entrada entrada;

	public MenuAdminTorneos(ConsolaAdministrador consola) {
		this.consola = consola;
		this.entrada = consola.entrada;
	}

	public Menu construir() {
		return new Menu("Torneos", entrada)
				.agregar("Crear torneo", this::crearTorneo)
				.agregar("Ver torneos por estado", this::verPorEstado)
				.agregar("Ver historial de un torneo", this::verHistorial)
				.agregar("Liberar cupos preferenciales sin usar", this::liberarPreferenciales)
				.agregar("Iniciar torneo", this::iniciar)
				.agregar("Programar encuentros", this::programarEncuentros)
				.agregar("Registrar resultado de un encuentro", this::registrarResultado)
				.agregar("Finalizar torneo", this::finalizar);
	}

	// ------------------------------------------------------------------

	private void crearTorneo() throws ReglaNegocioException {
		String nombre = entrada.texto("Nombre del torneo");
		if (consola.club.buscarTorneo(nombre) != null) {
			throw new ReglaNegocioException("Ya existe un torneo con ese nombre");
		}
		boolean competitivo = entrada.siNo("Es competitivo? (n = recreativo)");
		Especialidad especialidad = leerEspecialidad();
		int cupos = entrada.entero("Numero de cupos", 2, 128);
		LocalDate hoy = consola.hoy();
		LocalDate inicio = leerFechaDesde("Fecha de inicio", hoy.plusDays(1));
		LocalDate limite = leerFechaDesde("Ultimo dia de reserva de cupos preferenciales", hoy);
		while (!limite.isBefore(inicio)) {
			System.out.println("  Debe ser anterior a la fecha de inicio (" + inicio + ").");
			limite = leerFechaDesde("Ultimo dia de reserva de cupos preferenciales", hoy);
		}

		Torneo t;
		if (competitivo) {
			double tarifa = entrada.decimal("Tarifa de inscripcion", 1, 100000000);
			t = new TorneoCompetitivo(nombre, especialidad, cupos, inicio, limite, tarifa);
		} else {
			double bono = entrada.decimal("Porcentaje de descuento del bono (1-100)", 1, 100);
			t = new TorneoRecreativo(nombre, especialidad, cupos, inicio, limite, bono);
		}
		consola.club.registrarTorneo(t);
		System.out.println("Torneo creado: " + VistaTorneos.resumen(t));
		System.out.println("Cupos preferenciales reservados: " + t.getCuposPreferenciales());
	}

	private Especialidad leerEspecialidad() {
		FormatoTorneo formato = entrada.elegirEnum("Formato", FormatoTorneo.values());
		if (formato == FormatoTorneo.CONJUNTO) {
			DeporteConjunto deporte = entrada.elegirEnum("Deporte", DeporteConjunto.values());
			CategoriaEdad categoria = entrada.elegirEnum("Categoria", CategoriaEdad.values());
			return new EspecialidadConjunto(deporte, categoria);
		}
		Disciplina disciplina = entrada.elegirEnum("Disciplina", Disciplina.values());
		Nivel nivel = entrada.elegirEnum("Nivel", Nivel.values());
		return new EspecialidadIndividual(disciplina, nivel, formato == FormatoTorneo.DOBLES);
	}

	private LocalDate leerFechaDesde(String mensaje, LocalDate desde) {
		LocalDate fecha = entrada.fecha(mensaje);
		while (fecha.isBefore(desde)) {
			System.out.println("  La fecha no puede ser anterior a " + desde + ".");
			fecha = entrada.fecha(mensaje);
		}
		return fecha;
	}

	// ------------------------------------------------------------------

	private void verPorEstado() {
		List<String> opciones = new ArrayList<String>();
		opciones.add("Todos");
		for (EstadoTorneo e : EstadoTorneo.values()) {
			opciones.add(e.getNombre());
		}
		System.out.println("Que torneos quiere ver?");
		for (int i = 0; i < opciones.size(); i++) {
			System.out.println("  " + (i + 1) + ". " + opciones.get(i));
		}
		int op = entrada.entero("Seleccione", 1, opciones.size());
		if (op == 1) {
			VistaTorneos.imprimirLista(consola.club.getTorneos());
		} else {
			VistaTorneos.imprimirLista(consola.club.torneosPorEstado(EstadoTorneo.values()[op - 2]));
		}
	}

	private void verHistorial() throws ReglaNegocioException {
		Torneo t = elegirTorneo(consola.club.getTorneos());
		VistaTorneos.imprimirHistorial(t, consola.hoy());
	}

	private void liberarPreferenciales() throws ReglaNegocioException {
		Torneo t = elegirTorneo(consola.club.torneosPorEstado(EstadoTorneo.INSCRIPCIONES_ABIERTAS));
		int libres = t.cuposPreferencialesDisponibles(consola.hoy());
		if (libres == 0) {
			System.out.println("No hay cupos preferenciales sin usar (o ya se liberaron).");
			return;
		}
		if (entrada.siNo("Liberar " + libres + " cupo(s) preferencial(es) como regulares?")) {
			t.liberarCuposPreferenciales();
			System.out.println("Listo. Cupos regulares disponibles: " + t.cuposRegularesDisponibles(consola.hoy()));
		}
	}

	private void iniciar() throws ReglaNegocioException {
		Torneo t = elegirTorneo(consola.club.torneosPorEstado(EstadoTorneo.INSCRIPCIONES_ABIERTAS));
		t.iniciar(consola.hoy());
		System.out.println(t.getNombre() + " esta ahora " + t.getEstado().getNombre());
	}

	private void programarEncuentros() throws ReglaNegocioException {
		Torneo t = elegirTorneo(consola.club.torneosPorEstado(EstadoTorneo.EN_CURSO));
		if (entrada.siNo("Generar todos contra todos? (n = programar un encuentro)")) {
			LocalDate fecha = leerFechaDesde("Fecha de los encuentros", t.getFechaInicio());
			int n = t.generarTodosContraTodos(fecha).size();
			System.out.println("Se programaron " + n + " encuentros.");
			return;
		}
		InscripcionTorneo a = entrada.elegir("Participante A", t.getInscripciones(), i -> i.getParticipante().getNombre());
		InscripcionTorneo b = entrada.elegir("Participante B", t.getInscripciones(), i -> i.getParticipante().getNombre());
		String ronda = entrada.texto("Ronda (ej. Cuartos, Semifinal, Final)");
		LocalDate fecha = leerFechaDesde("Fecha", t.getFechaInicio());
		EncuentroTorneo e = t.programarEncuentro(a, b, fecha, ronda);
		System.out.println("Programado: " + VistaTorneos.describirEncuentro(e));
	}

	private void registrarResultado() throws ReglaNegocioException {
		Torneo t = elegirTorneo(consola.club.torneosPorEstado(EstadoTorneo.EN_CURSO));
		List<EncuentroTorneo> pendientes = new ArrayList<EncuentroTorneo>();
		for (EncuentroTorneo e : t.getEncuentros()) {
			if (!e.isJugado()) {
				pendientes.add(e);
			}
		}
		EncuentroTorneo e = entrada.elegir("Encuentros pendientes", pendientes, VistaTorneos::describirEncuentro);
		int ma = entrada.entero("Marcador de " + e.getParticipanteA().getParticipante().getNombre(), 0, 999);
		int mb = entrada.entero("Marcador de " + e.getParticipanteB().getParticipante().getNombre(), 0, 999);
		t.registrarResultado(e, ma, mb);
		System.out.println("Resultado registrado: " + VistaTorneos.describirEncuentro(e));
	}

	private void finalizar() throws ReglaNegocioException {
		Torneo t = elegirTorneo(consola.club.torneosPorEstado(EstadoTorneo.EN_CURSO));
		if (!entrada.siNo("Finalizar " + t.getNombre() + "? Ya no se podran registrar resultados")) {
			return;
		}
		t.finalizar();
		System.out.println("Torneo finalizado. Ganador: " + t.getGanador().getParticipante().getNombre());
		System.out.println("Premio: " + t.describirPremio());
	}

	// ------------------------------------------------------------------

	private Torneo elegirTorneo(List<Torneo> torneos) throws ReglaNegocioException {
		return entrada.elegir("Torneos", torneos, VistaTorneos::resumen);
	}
}
