package cds.ventas;

import cds.ReglaNegocioException;
import cds.usuarios.Usuario;

public class Bono {
	private final Usuario dueno;
	private final double porcentaje;
	private final String origen;
	private boolean usado;

	public Bono(Usuario dueno, double porcentaje, String origen) {
		this.dueno = dueno;
		this.porcentaje = porcentaje;
		this.origen = origen;
		usado = false;
	}

	public double calcularDescuento(double monto) {
		return monto * porcentaje / 100;
	}

	public void marcarUsado() throws ReglaNegocioException {
		if (usado) {
			throw new ReglaNegocioException("Este bono ya fue usado");
		}
		usado = true;
	}

	public void restaurarUsado(boolean usado) {
		this.usado = usado;
	}

	public Usuario getDueno() {
		return dueno;
	}

	public double getPorcentaje() {
		return porcentaje;
	}

	public String getOrigen() {
		return origen;
	}

	public boolean isUsado() {
		return usado;
	}
}
