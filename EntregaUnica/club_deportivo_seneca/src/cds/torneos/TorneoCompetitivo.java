package cds.torneos;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cds.ReglaNegocioException;
import cds.usuarios.Empleado;
import cds.usuarios.Usuario;

public class TorneoCompetitivo extends Torneo {

	public static final double FRACCION_PREMIO = 0.7;

	private final double tarifa;
	private final Map<Usuario, Double> premiosEntregados;

	public TorneoCompetitivo(String nombre, Especialidad especialidad, int cupos, LocalDate fechaInicio,
			LocalDate fechaLimitePreferencial, double tarifa) throws ReglaNegocioException {
		super(nombre, especialidad, cupos, fechaInicio, fechaLimitePreferencial);
		if (tarifa <= 0) {
			throw new ReglaNegocioException("Un torneo competitivo debe tener tarifa de inscripcion mayor que 0");
		}
		this.tarifa = tarifa;
		premiosEntregados = new LinkedHashMap<Usuario, Double>();
	}

	@Override
	public String getTipo() {
		return "Competitivo";
	}

	@Override
	public double tarifaPara(Participante participante) {
		List<Usuario> integrantes = participante.getIntegrantes();
		if (integrantes.isEmpty()) {
			return tarifa;
		}
		return tarifa * noEmpleados(integrantes).size() / integrantes.size();
	}

	private static List<Usuario> noEmpleados(List<Usuario> usuarios) {
		List<Usuario> lista = new ArrayList<Usuario>();
		for (Usuario u : usuarios) {
			if (!(u instanceof Empleado)) {
				lista.add(u);
			}
		}
		return lista;
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
		return "Premio en efectivo de $" + String.format("%,.0f", premioEfectivo()) + " (70% de lo recaudado)";
	}
	
	@Override
	protected void entregarPremio(InscripcionTorneo ganador) throws ReglaNegocioException {
		List<Usuario> premiados = noEmpleados(ganador.getParticipante().getIntegrantes());
		if (premiados.isEmpty()) {
			return;
		}
		double porPersona = premioEfectivo() / premiados.size();
		for (Usuario u : premiados) {
			premiosEntregados.put(u, porPersona);
		}
	}

	public void restaurarPremio(Usuario u, double monto) {
		premiosEntregados.put(u, monto);
	}

	public Map<Usuario, Double> getPremiosEntregados() {
		return new LinkedHashMap<Usuario, Double>(premiosEntregados);
	}

	public double getTarifa() {
		return tarifa;
	}
}
