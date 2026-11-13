package cds.torneos;

import java.time.LocalDate;

import cds.ReglaNegocioException;

/**
 * Torneo con tarifa de inscripcion. El ganador recibe un premio en efectivo
 * igual a lo recaudado en inscripciones.
 */
public class TorneoCompetitivo extends Torneo {
	/** Fraccion de lo recaudado que se entrega como premio (1.0 = todo). */
	public static final double FRACCION_PREMIO = 1.0;

	private final double tarifa;

	public TorneoCompetitivo(String nombre, Especialidad especialidad, int cupos, LocalDate fechaInicio,
			LocalDate fechaLimitePreferencial, double tarifa) throws ReglaNegocioException {
		super(nombre, especialidad, cupos, fechaInicio, fechaLimitePreferencial);
		if (tarifa <= 0) {
			throw new ReglaNegocioException("Un torneo competitivo debe tener tarifa de inscripcion mayor que 0");
		}
		this.tarifa = tarifa;
	}

	@Override
	public String getTipo() {
		return "Competitivo";
	}

	@Override
	protected double tarifaPara(Participante p) {
		// TODO (Persona B): los empleados no pagan (si todos los integrantes
		// son Empleado, retornar 0).
		return tarifa;
	}

	public double totalRecaudado() {
		double total = 0;
		for (InscripcionTorneo i : getInscripciones()) {
			total += i.getMontoPagado();
		}
		return total;
	}

	public double premioEfectivo() {
		return totalRecaudado() * FRACCION_PREMIO;
	}

	@Override
	public String describirPremio() {
		return "Premio en efectivo de $" + String.format("%,.0f", premioEfectivo()) + " (lo recaudado)";
	}

	@Override
	protected void entregarPremio(InscripcionTorneo ganador) throws ReglaNegocioException {
		// TODO (Persona B): registrar el premio en efectivo para el ganador.
		// Los empleados no reciben premio en efectivo.
	}

	public double getTarifa() {
		return tarifa;
	}
}
