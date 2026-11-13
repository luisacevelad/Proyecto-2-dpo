package cds.torneos;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cds.ReglaNegocioException;
import cds.usuarios.Socio;
import cds.usuarios.Usuario;


public abstract class Torneo {

	public static final int PORCENTAJE_PREFERENCIAL = 20;

	/** Dias antes del inicio en que se liberan los cupos preferenciales no usados. */
	public static final int DIAS_LIBERACION = 3;

	private final String nombre;
	private final Especialidad especialidad;
	private final int cupos;
	private final LocalDate fechaInicio;
	private final LocalDate fechaLimitePreferencial;
	private EstadoTorneo estado;
	private boolean preferencialesLiberados;
	private final List<InscripcionTorneo> inscripciones;
	private final List<EncuentroTorneo> encuentros;
	private InscripcionTorneo ganador;

	protected Torneo(String nombre, Especialidad especialidad, int cupos, LocalDate fechaInicio,
			LocalDate fechaLimitePreferencial) throws ReglaNegocioException {
		if (nombre == null || nombre.isBlank()) {
			throw new ReglaNegocioException("El torneo debe tener nombre");
		}
		if (especialidad == null) {
			throw new ReglaNegocioException("El torneo debe tener deporte o disciplina");
		}
		if (cupos < 2) {
			throw new ReglaNegocioException("Un torneo necesita al menos 2 cupos");
		}
		if (fechaInicio == null || fechaLimitePreferencial == null) {
			throw new ReglaNegocioException("El torneo debe tener fecha de inicio y fecha limite de cupos preferenciales");
		}
		if (!fechaLimitePreferencial.isBefore(fechaInicio)) {
			throw new ReglaNegocioException("La fecha limite de cupos preferenciales debe ser anterior al inicio");
		}
		this.nombre = nombre;
		this.especialidad = especialidad;
		this.cupos = cupos;
		this.fechaInicio = fechaInicio;
		this.fechaLimitePreferencial = fechaLimitePreferencial;
		estado = EstadoTorneo.INSCRIPCIONES_ABIERTAS;
		preferencialesLiberados = false;
		inscripciones = new ArrayList<InscripcionTorneo>();
		encuentros = new ArrayList<EncuentroTorneo>();
		ganador = null;
	}

	public abstract String getTipo();

	public abstract double tarifaPara(Participante p);

	public abstract String describirPremio();

	protected abstract void entregarPremio(InscripcionTorneo ganador) throws ReglaNegocioException;

	public int getCuposPreferenciales() {
		return (cupos * PORCENTAJE_PREFERENCIAL + 99) / 100;
	}

	public boolean preferencialesVigentes(LocalDate hoy) {
		return !preferencialesLiberados && !hoy.isAfter(fechaLimitePreferencial);
	}

	public int contarPreferencialesOcupados() {
		int n = 0;
		for (InscripcionTorneo i : inscripciones) {
			if (i.isPreferencial()) {
				n++;
			}
		}
		return n;
	}

	public int contarRegularesOcupados() {
		return inscripciones.size() - contarPreferencialesOcupados();
	}

	public int cuposPreferencialesDisponibles(LocalDate hoy) {
		if (!preferencialesVigentes(hoy)) {
			return 0;
		}
		return getCuposPreferenciales() - contarPreferencialesOcupados();
	}

	public int cuposRegularesDisponibles(LocalDate hoy) {
		if (preferencialesVigentes(hoy)) {
			return cupos - getCuposPreferenciales() - contarRegularesOcupados();
		}

		return cupos - inscripciones.size();
	}

	public int cuposDisponibles() {
		return cupos - inscripciones.size();
	}

	public void liberarCuposPreferenciales() throws ReglaNegocioException {
		if (estado != EstadoTorneo.INSCRIPCIONES_ABIERTAS) {
			throw new ReglaNegocioException("Las inscripciones de " + nombre + " ya cerraron");
		}
		preferencialesLiberados = true;
	}


	public boolean inscripcionesAbiertas(LocalDate hoy) {
		return estado == EstadoTorneo.INSCRIPCIONES_ABIERTAS && hoy.isBefore(fechaInicio);
	}


	public InscripcionTorneo inscribir(Participante p, LocalDate hoy, boolean tieneRankingPreferencial)
			throws ReglaNegocioException {
		if (!inscripcionesAbiertas(hoy)) {
			throw new ReglaNegocioException("Las inscripciones de " + nombre + " estan cerradas");
		}
		especialidad.validar(p);
		validarIntegrantes(p);

		boolean preferencial;
		if (tieneRankingPreferencial && cuposPreferencialesDisponibles(hoy) > 0) {
			preferencial = true;
		} else if (cuposRegularesDisponibles(hoy) > 0) {
			preferencial = false;
		} else if (cuposPreferencialesDisponibles(hoy) > 0) {
			throw new ReglaNegocioException("Los cupos regulares de " + nombre + " se agotaron. Los "
					+ cuposPreferencialesDisponibles(hoy) + " que quedan estan reservados para los mejores del ranking hasta el "
					+ fechaLimitePreferencial);
		} else {
			throw new ReglaNegocioException(nombre + " no tiene cupos disponibles");
		}

		InscripcionTorneo nueva = new InscripcionTorneo(p, hoy, preferencial, tarifaPara(p));
		inscripciones.add(nueva);
		return nueva;
	}

	private void validarIntegrantes(Participante p) throws ReglaNegocioException {
		for (Usuario u : p.getIntegrantes()) {
			if (buscarInscripcion(u) != null) {
				throw new ReglaNegocioException(u.getNombre() + " ya esta inscrito en " + nombre);
			}
			if (!(p instanceof EquipoTorneo) && u instanceof Socio && !((Socio) u).estaHabilitado()) {
				throw new ReglaNegocioException(u.getNombre() + " no esta habilitado (inactivo o con restriccion medica)");
			}
		}
		if (p instanceof EquipoTorneo) {
			for (InscripcionTorneo i : inscripciones) {
				Participante otro = i.getParticipante();
				if (otro instanceof EquipoTorneo && ((EquipoTorneo) otro).getEquipo() == ((EquipoTorneo) p).getEquipo()) {
					throw new ReglaNegocioException(p.getNombre() + " ya esta inscrito en " + nombre);
				}
			}
		}
	}

	public void desinscribir(InscripcionTorneo inscripcion, LocalDate hoy) throws ReglaNegocioException {
		if (!inscripcionesAbiertas(hoy)) {
			throw new ReglaNegocioException("No es posible salirse: " + nombre + " ya inicio");
		}
		if (!inscripciones.remove(inscripcion)) {
			throw new ReglaNegocioException("Esa inscripcion no pertenece a " + nombre);
		}
	}

	public InscripcionTorneo buscarInscripcion(Usuario u) {
		for (InscripcionTorneo i : inscripciones) {
			if (i.getParticipante().incluye(u)) {
				return i;
			}
		}
		return null;
	}

	public InscripcionTorneo buscarInscripcion(String nombreParticipante) {
		for (InscripcionTorneo i : inscripciones) {
			if (i.getParticipante().getNombre().equalsIgnoreCase(nombreParticipante)) {
				return i;
			}
		}
		return null;
	}

	public void iniciar(LocalDate hoy) throws ReglaNegocioException {
		if (estado != EstadoTorneo.INSCRIPCIONES_ABIERTAS) {
			throw new ReglaNegocioException(nombre + " ya esta " + estado.getNombre());
		}
		if (hoy.isBefore(fechaInicio)) {
			throw new ReglaNegocioException(nombre + " empieza el " + fechaInicio);
		}
		if (inscripciones.size() < 2) {
			throw new ReglaNegocioException(nombre + " necesita al menos 2 inscritos para iniciar");
		}
		estado = EstadoTorneo.EN_CURSO;
	}

	public EncuentroTorneo programarEncuentro(InscripcionTorneo a, InscripcionTorneo b, LocalDate fecha, String ronda)
			throws ReglaNegocioException {
		validarEnCurso();
		if (!inscripciones.contains(a) || !inscripciones.contains(b)) {
			throw new ReglaNegocioException("Los dos participantes deben estar inscritos en " + nombre);
		}
		if (fecha.isBefore(fechaInicio)) {
			throw new ReglaNegocioException("El encuentro no puede ser antes del inicio del torneo (" + fechaInicio + ")");
		}
		EncuentroTorneo e = new EncuentroTorneo(a, b, fecha, ronda);
		encuentros.add(e);
		return e;
	}

	public List<EncuentroTorneo> generarTodosContraTodos(LocalDate fecha) throws ReglaNegocioException {
		validarEnCurso();
		if (!encuentros.isEmpty()) {
			throw new ReglaNegocioException(nombre + " ya tiene encuentros programados");
		}
		List<EncuentroTorneo> nuevos = new ArrayList<EncuentroTorneo>();
		for (int i = 0; i < inscripciones.size(); i++) {
			for (int j = i + 1; j < inscripciones.size(); j++) {
				nuevos.add(programarEncuentro(inscripciones.get(i), inscripciones.get(j), fecha, "Todos contra todos"));
			}
		}
		return nuevos;
	}

	public void registrarResultado(EncuentroTorneo e, int marcadorA, int marcadorB) throws ReglaNegocioException {
		validarEnCurso();
		if (!encuentros.contains(e)) {
			throw new ReglaNegocioException("Ese encuentro no pertenece a " + nombre);
		}
		e.registrarResultado(marcadorA, marcadorB, especialidad.permiteEmpate());
	}

	public List<Posicion> tablaPosiciones() {
		List<Posicion> tabla = new ArrayList<Posicion>();
		for (InscripcionTorneo i : inscripciones) {
			tabla.add(new Posicion(i));
		}
		for (EncuentroTorneo e : encuentros) {
			if (e.isJugado()) {
				for (Posicion pos : tabla) {
					if (pos.getInscripcion() == e.getParticipanteA()) {
						pos.sumarEncuentro(e.getMarcadorA(), e.getMarcadorB());
					} else if (pos.getInscripcion() == e.getParticipanteB()) {
						pos.sumarEncuentro(e.getMarcadorB(), e.getMarcadorA());
					}
				}
			}
		}
		Collections.sort(tabla, Comparator.comparingInt(Posicion::getPuntos).reversed()
				.thenComparing(Comparator.comparingInt(Posicion::getDiferencia).reversed())
				.thenComparing(Comparator.comparingInt(Posicion::getAFavor).reversed()));
		return tabla;
	}

	public void finalizar() throws ReglaNegocioException {
		validarEnCurso();
		if (encuentros.isEmpty()) {
			throw new ReglaNegocioException(nombre + " no tiene encuentros");
		}
		for (EncuentroTorneo e : encuentros) {
			if (!e.isJugado()) {
				throw new ReglaNegocioException("Faltan encuentros por jugar en " + nombre);
			}
		}
		List<Posicion> tabla = tablaPosiciones();
		Posicion primero = tabla.get(0);
		Posicion segundo = tabla.get(1);
		if (primero.getPuntos() == segundo.getPuntos() && primero.getDiferencia() == segundo.getDiferencia()
				&& primero.getAFavor() == segundo.getAFavor()) {
			throw new ReglaNegocioException("Hay empate en el primer lugar entre " + primero.getParticipante().getNombre()
					+ " y " + segundo.getParticipante().getNombre() + ". Programe un encuentro de desempate");
		}
		ganador = primero.getInscripcion();
		estado = EstadoTorneo.FINALIZADO;
		entregarPremio(ganador);
	}

	private void validarEnCurso() throws ReglaNegocioException {
		if (estado != EstadoTorneo.EN_CURSO) {
			throw new ReglaNegocioException(nombre + " no esta en curso (estado: " + estado.getNombre() + ")");
		}
	}

	public void restaurarInscripcion(InscripcionTorneo i) {
		inscripciones.add(i);
	}

	public void restaurarEncuentro(EncuentroTorneo e) {
		encuentros.add(e);
	}

	public void restaurarEstado(EstadoTorneo estado, boolean preferencialesLiberados, InscripcionTorneo ganador) {
		this.estado = estado;
		this.preferencialesLiberados = preferencialesLiberados;
		this.ganador = ganador;
	}


	public String getNombre() {
		return nombre;
	}

	public Especialidad getEspecialidad() {
		return especialidad;
	}

	public FormatoTorneo getFormato() {
		return especialidad.getFormato();
	}

	public int getCupos() {
		return cupos;
	}

	public LocalDate getFechaInicio() {
		return fechaInicio;
	}

	public LocalDate getFechaLimitePreferencial() {
		return fechaLimitePreferencial;
	}

	public EstadoTorneo getEstado() {
		return estado;
	}

	public boolean isPreferencialesLiberados() {
		return preferencialesLiberados;
	}

	public List<InscripcionTorneo> getInscripciones() {
		return new ArrayList<InscripcionTorneo>(inscripciones);
	}

	public List<EncuentroTorneo> getEncuentros() {
		return new ArrayList<EncuentroTorneo>(encuentros);
	}

	public InscripcionTorneo getGanador() {
		return ganador;
	}
}
