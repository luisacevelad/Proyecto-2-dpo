package cds.torneos;

import java.time.LocalDate;

import cds.ReglaNegocioException;

/**
 * Torneo sin costo de inscripcion. El ganador recibe un bono de descuento
 * redimible en la tienda o la cafeteria.
 */
public class TorneoRecreativo extends Torneo {
	private final double porcentajeBono;

	/**
	 * @param porcentajeBono descuento del bono, entre 1 y 100 (p. ej. 20 = 20%).
	 */
	public TorneoRecreativo(String nombre, Especialidad especialidad, int cupos, LocalDate fechaInicio,
			LocalDate fechaLimitePreferencial, double porcentajeBono) throws ReglaNegocioException {
		super(nombre, especialidad, cupos, fechaInicio, fechaLimitePreferencial);
		if (porcentajeBono <= 0 || porcentajeBono > 100) {
			throw new ReglaNegocioException("El porcentaje del bono debe estar entre 1 y 100");
		}
		this.porcentajeBono = porcentajeBono;
	}

	@Override
	public String getTipo() {
		return "Recreativo";
	}

	@Override
	protected double tarifaPara(Participante p) {
		return 0;
	}

	@Override
	public String describirPremio() {
		return "Bono de " + porcentajeBono + "% de descuento en tienda o cafeteria";
	}

	@Override
	protected void entregarPremio(InscripcionTorneo ganador) throws ReglaNegocioException {
		// TODO (Persona B): crear un Bono de porcentajeBono para cada integrante
		// del ganador que sea Socio y guardarlo en su lista de bonos.
	}

	public double getPorcentajeBono() {
		return porcentajeBono;
	}
}
