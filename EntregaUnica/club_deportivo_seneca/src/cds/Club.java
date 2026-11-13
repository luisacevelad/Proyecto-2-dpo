package cds;

import java.time.DayOfWeek;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import cds.academias.Academia;
import cds.academias.InscripcionAcademia;
import cds.academias.Instalacion;
import cds.academias.Modalidad;
import cds.academias.Reserva;
import cds.academias.RegistroRanking;
import cds.competencias.Competencia;
import cds.entrenamiento.Asistencia;
import cds.entrenamiento.EstadoAsistencia;
import cds.entrenamiento.SesionEntrenamiento;
import cds.usuarios.Administrador;
import cds.usuarios.Empleado;
import cds.usuarios.Entrenador;
import cds.usuarios.EstadoSolicitud;
import cds.usuarios.Fisioterapeuta;
import cds.usuarios.RestriccionMedica;
import cds.usuarios.SolicitudCambioTurno;
import cds.usuarios.Socio;
import cds.usuarios.Turno;
import cds.usuarios.Usuario;
import cds.conjunto.DeporteConjunto;
import cds.conjunto.Equipo;
import cds.conjunto.PartidoInterno;
import cds.partidos.Partido;
import cds.ventas.Mensualidad;
import cds.ventas.Rubro;
import cds.ventas.ResumenVentas;
import cds.ventas.TiendaCafeteria;
import cds.ventas.Venta;
import cds.torneos.EstadoTorneo;
import cds.torneos.InscripcionTorneo;
import cds.torneos.Participante;
import cds.torneos.RankingTorneos;
import cds.torneos.Torneo;
import cds.torneos.FormatoTorneo;
import cds.torneos.TorneoRecreativo;
import cds.ventas.Bono;

public class Club {
	private final List<Usuario> usuarios;
	private final List<Equipo> equipos;
	private final List<Partido> partidos;
	private final TiendaCafeteria tienda;
	private final List<Academia> academias;
	private final List<Competencia> competencias;
	private final List<SesionEntrenamiento> sesiones;
	private final List<Turno> turnos;
	private final List<SolicitudCambioTurno> solicitudes;
	private final List<Torneo> torneos;

	public static final LocalTime APERTURA = LocalTime.of(6, 0);
	public static final LocalTime CIERRE = LocalTime.of(22, 0);

	public Club() {
		usuarios = new ArrayList<Usuario>();
		equipos = new ArrayList<Equipo>();
		partidos = new ArrayList<Partido>();
		tienda = new TiendaCafeteria();
		academias = new ArrayList<Academia>();
		competencias = new ArrayList<Competencia>();
		sesiones = new ArrayList<SesionEntrenamiento>();
		turnos = new ArrayList<Turno>();
		solicitudes = new ArrayList<SolicitudCambioTurno>();
		torneos = new ArrayList<Torneo>();
	}


	
	public void registrarUsuario(Usuario u) throws ReglaNegocioException {
		if (buscarUsuario(u.getLogin()) != null) {
			throw new ReglaNegocioException("Ya existe un usuario con el login " + u.getLogin());
		}
		if (u instanceof Empleado) {
			Empleado otro = buscarEmpleadoPorCodigo(((Empleado) u).getCodigoDescuento());
			if (otro != null) {
				throw new ReglaNegocioException("El codigo de descuento " + otro.getCodigoDescuento() + " ya lo tiene " + otro.getNombre());
			}
		}
		usuarios.add(u);
	}

	/** Empleado dueno de un codigo de descuento (sin importar mayusculas), o null si no existe. */
	public Empleado buscarEmpleadoPorCodigo(String codigo) {
		if (codigo == null) {
			return null;
		}
		for (Usuario u : usuarios) {
			if (u instanceof Empleado && codigo.equalsIgnoreCase(((Empleado) u).getCodigoDescuento())) {
				return (Empleado) u;
			}
		}
		return null;
	}

	public Usuario buscarUsuario(String login) {
		for (Usuario u : usuarios) {
			if (u.getLogin().equals(login)) {
				return u;
			}
		}
		return null;
	}

	
	
	public Usuario autenticar(String login, String password) throws ReglaNegocioException {
		Usuario u = buscarUsuario(login);
		if (u == null || !u.verificarPassword(password)) {
			throw new ReglaNegocioException("Login o password incorrectos");
		}
		return u;
	}

	public void registrarEquipo(Equipo e) throws ReglaNegocioException {
		for (Equipo actual : equipos) {
			if (actual.getNombre().equals(e.getNombre())) {
				throw new ReglaNegocioException("Ya existe un equipo llamado " + e.getNombre());
			}
		}
		equipos.add(e);
	}

	public Equipo buscarEquipo(String nombre) {
		for (Equipo e : equipos) {
			if (e.getNombre().equals(nombre)) {
				return e;
			}
		}
		return null;
	}

	public void registrarPartido(Partido p) {
		partidos.add(p);
	}

	public void registrarAcademia(Academia a) throws ReglaNegocioException {
		if (buscarAcademia(a.getNombre()) != null) {
			throw new ReglaNegocioException("Ya existe la academia " + a.getNombre());
		}
		academias.add(a);
	}

	public Academia buscarAcademia(String nombre) {
		for (Academia a : academias) {
			if (a.getNombre().equals(nombre)) {
				return a;
			}
		}
		return null;
	}

	public void registrarCompetencia(Competencia c) throws ReglaNegocioException {
		if (buscarCompetencia(c.getNombre()) != null) {
			throw new ReglaNegocioException("Ya existe la competencia " + c.getNombre());
		}
		competencias.add(c);
	}

	public Competencia buscarCompetencia(String nombre) {
		for (Competencia c : competencias) {
			if (c.getNombre().equals(nombre)) {
				return c;
			}
		}
		return null;
	}

	public void registrarSesion(SesionEntrenamiento s) {
		sesiones.add(s);
	}

	public void registrarTurno(Turno t) {
		turnos.add(t);
	}

	public SolicitudCambioTurno solicitarCambioTurno(Empleado solicitante, Turno turno, DayOfWeek nuevoDia,
			LocalTime nuevaHoraInicio, LocalTime nuevaHoraFin) throws ReglaNegocioException {
		if (turno.getEmpleado() != solicitante) {
			throw new ReglaNegocioException("Solo se puede pedir el cambio de un turno propio");
		}
		if (!nuevaHoraFin.isAfter(nuevaHoraInicio)) {
			throw new ReglaNegocioException("La hora de fin debe ser posterior a la hora de inicio");
		}
		SolicitudCambioTurno s = new SolicitudCambioTurno(solicitante, turno, nuevoDia, nuevaHoraInicio, nuevaHoraFin);
		solicitudes.add(s);
		return s;
	}

	public void resolverSolicitud(Administrador admin, SolicitudCambioTurno s, boolean aprobar) throws ReglaNegocioException {
		validarAdministrador(admin);
		if (s.getEstado() != EstadoSolicitud.PENDIENTE) {
			throw new ReglaNegocioException("La solicitud ya fue resuelta: " + s.getEstado());
		}
		if (!aprobar) {
			s.resolver(admin, EstadoSolicitud.RECHAZADA);
			return;
		}
		Turno t = s.getTurno();
		DayOfWeek diaViejo = t.getDia();
		LocalTime inicioViejo = t.getHoraInicio();
		LocalTime finViejo = t.getHoraFin();
		t.cambiar(s.getNuevoDia(), s.getNuevaHoraInicio(), s.getNuevaHoraFin());
		String falta = horaSinCobertura(diaViejo);
		if (falta == null) {
			falta = horaSinCobertura(s.getNuevoDia());
		}
		if (falta != null) {
			t.cambiar(diaViejo, inicioViejo, finViejo);
			s.resolver(admin, EstadoSolicitud.RECHAZADA);
			throw new ReglaNegocioException("El cambio deja sin cobertura (2 entrenadores y 1 fisioterapeuta) el " + falta);
		}
		s.resolver(admin, EstadoSolicitud.APROBADA);
	}

	private String horaSinCobertura(DayOfWeek dia) {
		for (LocalTime h = APERTURA; h.isBefore(CIERRE); h = h.plusHours(1)) {
			int entrenadores = 0;
			int fisios = 0;
			for (Turno t : turnos) {
				if (t.cubre(dia, h)) {
					if (t.getEmpleado() instanceof Entrenador) {
						entrenadores++;
					} else if (t.getEmpleado() instanceof Fisioterapeuta) {
						fisios++;
					}
				}
			}
			if (entrenadores < 2 || fisios < 1) {
				return dia + " a las " + h;
			}
		}
		return null;
	}

	public double totalVentas(Rubro rubro, LocalDate desde, LocalDate hasta) {
		double total = 0;
		for (Venta v : tienda.getVentas()) {
			if (v.getRubro() == rubro && v.isConfirmada() && !v.getFecha().isBefore(desde) && !v.getFecha().isAfter(hasta)) {
				total += v.total();
			}
		}
		if (rubro == Rubro.MENSUALIDAD) {
			for (Mensualidad m : tienda.getMensualidades()) {
				if (m.isPagada() && !m.getFechaPago().isBefore(desde) && !m.getFechaPago().isAfter(hasta)) {
					total += m.getMonto();
				}
			}
		}
		return total;
	}

	public int convocatorias(Socio socio) {
		int total = 0;
		for (SesionEntrenamiento s : sesiones) {
			if (s.isFinalizado()) {
				for (Asistencia a : s.getConvocados()) {
					if (a.getSocio() == socio) {
						total++;
					}
				}
			}
		}
		return total;
	}

	public int asistencias(Socio socio) {
		int total = 0;
		for (SesionEntrenamiento s : sesiones) {
			if (s.isFinalizado()) {
				for (Asistencia a : s.getConvocados()) {
					if (a.getSocio() == socio && (a.getEstado() == EstadoAsistencia.ASISTIO || a.getEstado() == EstadoAsistencia.LLEGO_TARDE)) {
						total++;
					}
				}
			}
		}
		return total;
	}

	public List<Partido> partidosJugados() {
		List<Partido> jugados = new ArrayList<Partido>();
		for (Partido p : partidos) {
			if (p.isJugado()) {
				jugados.add(p);
			}
		}
		return jugados;
	}

	public List<RegistroRanking> evolucionRanking(Academia academia, Socio socio) {
		InscripcionAcademia i = academia.getInscripcion(socio);
		if (i == null) {
			return new ArrayList<RegistroRanking>();
		}
		return i.getHistorial();
	}

	public Equipo buscarEquipoDe(Socio s, DeporteConjunto d) {
		for (Equipo e : equipos) {
			if (e.getDeporte() == d && e.tieneJugador(s)) {
				return e;
			}
		}
		return null;
	}

	public void inscribirJugador(Equipo e, Socio s, String posicion, int numero, LocalDate hoy) throws ReglaNegocioException {
		Equipo actual = buscarEquipoDe(s, e.getDeporte());
		if (actual != null) {
			throw new ReglaNegocioException(s.getNombre() + " ya juega " + e.getDeporte().getNombre() + " en " + actual.getNombre()
					+ "; solo puede pertenecer a un unico equipo por disciplina");
		}
		e.agregarJugador(s, posicion, numero, hoy);
	}

	public PartidoInterno registrarPartidoInterno(Equipo local, Equipo visitante, LocalDate fecha) throws ReglaNegocioException {
		PartidoInterno p = new PartidoInterno(local, visitante, fecha);
		partidos.add(p);
		return p;
	}

	public void registrarRestriccion(Fisioterapeuta fisio, Socio s, String motivo, LocalDate fecha) {
		s.agregarRestriccion(new RestriccionMedica(fisio, motivo, fecha));
	}


	public List<Usuario> getUsuarios() {
		return usuarios;
	}
	

	public List<Equipo> getEquipos() {
		return equipos;
	}

	public List<Partido> getPartidos() {
		return partidos;
	}

	public TiendaCafeteria getTienda() {
		return tienda;
	}

	public List<Academia> getAcademias() {
		return academias;
	}

	public List<Competencia> getCompetencias() {
		return competencias;
	}

	public List<SesionEntrenamiento> getSesiones() {
		return sesiones;
	}

	public List<Turno> getTurnos() {
		return turnos;
	}

	public List<SolicitudCambioTurno> getSolicitudes() {
		return solicitudes;
	}

	// ------------------------------------------------------------------
	// Autorizacion
	// ------------------------------------------------------------------

	/**
	 * Una operacion administrativa no se autoriza solo por recibir un objeto Administrador:
	 * debe ser exactamente el administrador registrado en este club con ese login (el mismo
	 * objeto que devuelve autenticar). Asi un Administrador creado por fuera del club no
	 * puede aprobar nada.
	 */
	public void validarAdministrador(Administrador admin) throws ReglaNegocioException {
		if (admin == null || buscarUsuario(admin.getLogin()) != admin) {
			throw new ReglaNegocioException("Operacion no autorizada: se requiere un administrador autenticado en el club");
		}
	}

	// ------------------------------------------------------------------
	// Reglas de operacion que usan las consolas (Proyecto 2)
	// ------------------------------------------------------------------

	/** Valida que un horario este dentro de la atencion del club y que la hora de fin sea posterior. */
	private void validarHorarioClub(LocalTime inicio, LocalTime fin) throws ReglaNegocioException {
		if (!fin.isAfter(inicio)) {
			throw new ReglaNegocioException("La hora de fin debe ser posterior a la hora de inicio");
		}
		if (inicio.isBefore(APERTURA) || fin.isAfter(CIERRE)) {
			throw new ReglaNegocioException("El club atiende de " + APERTURA + " a " + CIERRE);
		}
	}

	/**
	 * Reserva una instalacion de una academia. Ademas de las reglas de la academia,
	 * exige que no sea en el pasado y que este dentro del horario del club.
	 */
	public Reserva reservar(Academia academia, Instalacion instalacion, Socio socio, LocalDate fecha, LocalTime inicio,
			LocalTime fin, int jugadores, Modalidad modalidad, LocalDateTime ahora) throws ReglaNegocioException {
		if (fecha.isBefore(ahora.toLocalDate()) || (fecha.equals(ahora.toLocalDate()) && inicio.isBefore(ahora.toLocalTime()))) {
			throw new ReglaNegocioException("No se puede reservar en una fecha u hora que ya paso");
		}
		validarHorarioClub(inicio, fin);
		return academia.reservar(instalacion, socio, fecha, inicio, fin, jugadores, modalidad);
	}

	/** Crea y registra un turno validando el horario del club. */
	public Turno asignarTurno(Empleado empleado, DayOfWeek dia, LocalTime inicio, LocalTime fin) throws ReglaNegocioException {
		validarHorarioClub(inicio, fin);
		Turno turno = new Turno(empleado, dia, inicio, fin);
		registrarTurno(turno);
		return turno;
	}

	/** Agrega una instalacion a una academia; el nombre no se puede repetir en todo el club. */
	public void agregarInstalacion(Academia academia, Instalacion instalacion) throws ReglaNegocioException {
		for (Academia a : academias) {
			for (Instalacion i : a.getInstalaciones()) {
				if (i.getNombre().equalsIgnoreCase(instalacion.getNombre())) {
					throw new ReglaNegocioException("Ya existe una instalacion llamada " + instalacion.getNombre());
				}
			}
		}
		academia.agregarInstalacion(instalacion);
	}

	/** Genera el cobro mensual de un socio; no puede haber dos cobros del mismo mes. */
	public Mensualidad generarMensualidad(Socio socio, YearMonth mes, double monto) throws ReglaNegocioException {
		for (Mensualidad m : tienda.getMensualidades()) {
			if (m.getSocio() == socio && m.getMes().equals(mes)) {
				throw new ReglaNegocioException(socio.getNombre() + " ya tiene la mensualidad de " + mes);
			}
		}
		if (monto <= 0) {
			throw new ReglaNegocioException("El monto de la mensualidad debe ser mayor que 0");
		}
		Mensualidad mensualidad = new Mensualidad(socio, mes, monto);
		tienda.registrarMensualidad(mensualidad);
		return mensualidad;
	}

	/** Cantidad, total, impuestos y propinas de un rubro en un periodo (para el reporte de ingresos). */
	public ResumenVentas resumenVentas(Rubro rubro, LocalDate desde, LocalDate hasta) throws ReglaNegocioException {
		if (hasta.isBefore(desde)) {
			throw new ReglaNegocioException("La fecha final no puede ser anterior a la inicial");
		}
		int cantidad = 0;
		double impuestos = 0;
		double propinas = 0;
		for (Venta v : tienda.getVentas()) {
			if (v.getRubro() == rubro && v.isConfirmada() && !v.getFecha().isBefore(desde) && !v.getFecha().isAfter(hasta)) {
				cantidad++;
				impuestos += v.impuesto();
				propinas += v.cargosAdicionales();
			}
		}
		if (rubro == Rubro.MENSUALIDAD) {
			for (Mensualidad m : tienda.getMensualidades()) {
				if (m.isPagada() && !m.getFechaPago().isBefore(desde) && !m.getFechaPago().isAfter(hasta)) {
					cantidad++;
				}
			}
		}
		return new ResumenVentas(rubro, cantidad, totalVentas(rubro, desde, hasta), impuestos, propinas);
	}

	// ------------------------------------------------------------------
	// Torneos internos (Proyecto 2)
	// ------------------------------------------------------------------

	public void registrarTorneo(Torneo t) throws ReglaNegocioException {
		if (buscarTorneo(t.getNombre()) != null) {
			throw new ReglaNegocioException("Ya existe el torneo " + t.getNombre());
		}
		torneos.add(t);
	}

	public Torneo buscarTorneo(String nombre) {
		for (Torneo t : torneos) {
			if (t.getNombre().equalsIgnoreCase(nombre)) {
				return t;
			}
		}
		return null;
	}

	public List<Torneo> torneosPorEstado(EstadoTorneo estado) {
		List<Torneo> lista = new ArrayList<Torneo>();
		for (Torneo t : torneos) {
			if (t.getEstado() == estado) {
				lista.add(t);
			}
		}
		return lista;
	}


	public InscripcionTorneo inscribirEnTorneo(Torneo t, Participante p, LocalDate hoy) throws ReglaNegocioException {
		validarParticipanteTorneo(t, p, hoy);
		boolean preferencial = new RankingTorneos(this).esPreferencial(p, t);
		return t.inscribir(p, hoy, preferencial);
	}

	protected void validarParticipanteTorneo(Torneo t, Participante p, LocalDate hoy) throws ReglaNegocioException {
		for (Usuario u : p.getIntegrantes()) {
			if (u instanceof Administrador) {
				throw new ReglaNegocioException("Los administradores no pueden participar en torneos");
			}
			if (u instanceof Empleado) {
				if (t.getFormato() == FormatoTorneo.CONJUNTO) {
					throw new ReglaNegocioException(u.getNombre() + " es empleado: solo puede jugar torneos individuales");
				}
				DayOfWeek dia = t.getFechaInicio().getDayOfWeek();
				if (tieneTurno((Empleado) u, dia)) {
					throw new ReglaNegocioException(u.getNombre() + " tiene turno el " + dia + " (dia de inicio de " + t.getNombre() + ")");
				}
			}
		}
	}

	private boolean tieneTurno(Empleado e, DayOfWeek dia) {
		for (Turno turno : turnos) {
			if (turno.getEmpleado() == e && turno.getDia() == dia) {
				return true;
			}
		}
		return false;
	}

	public List<Bono> bonosDisponibles(Usuario u) {
		List<Bono> lista = new ArrayList<Bono>();
		for (Torneo t : torneos) {
			if (t instanceof TorneoRecreativo) {
				for (Bono b : ((TorneoRecreativo) t).getBonosEntregados()) {
					if (b.getDueno() == u && !b.isUsado()) {
						lista.add(b);
					}
				}
			}
		}
		return lista;
	}
	public void desinscribirDeTorneo(Torneo t, Usuario u, LocalDate hoy) throws ReglaNegocioException {
		InscripcionTorneo i = t.buscarInscripcion(u);
		if (i == null) {
			throw new ReglaNegocioException(u.getNombre() + " no esta inscrito en " + t.getNombre());
		}
		t.desinscribir(i, hoy);
	}

	public List<Torneo> getTorneos() {
		return torneos;
	}
}
