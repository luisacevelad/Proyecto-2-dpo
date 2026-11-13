package cds.torneos;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import cds.ReglaNegocioException;
import cds.usuarios.Usuario;
import cds.ventas.Bono;

public class TorneoRecreativo extends Torneo {
	private final double porcentajeBono;
	private final List<Bono> bonosEntregados;

	public TorneoRecreativo(String nombre, Especialidad especialidad, int cupos, LocalDate fechaInicio,
			LocalDate fechaLimitePreferencial, double porcentajeBono) throws ReglaNegocioException {
		super(nombre, especialidad, cupos, fechaInicio, fechaLimitePreferencial);
		if (porcentajeBono <= 0 || porcentajeBono > 100) {
			throw new ReglaNegocioException("El porcentaje del bono debe estar entre 1 y 100");
		}
		this.porcentajeBono = porcentajeBono;
		bonosEntregados = new ArrayList<Bono>();
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
		for (Usuario u : ganador.getParticipante().getIntegrantes()) {
			bonosEntregados.add(new Bono(u, porcentajeBono, getNombre()));
		}
	}

	public void restaurarBono(Bono b) {
		bonosEntregados.add(b);
	}

	public List<Bono> getBonosEntregados() {
		return new ArrayList<Bono>(bonosEntregados);
	}

	public double getPorcentajeBono() {
		return porcentajeBono;
	}
}
