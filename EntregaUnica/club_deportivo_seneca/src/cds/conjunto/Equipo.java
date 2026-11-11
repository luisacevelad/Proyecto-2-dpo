package cds.conjunto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cds.usuarios.RestriccionMedica;
import cds.usuarios.Socio;
import cds.ReglaNegocioException;
import cds.partidos.Suspension;

public class Equipo {

	private final String nombre;
	private final DeporteConjunto deporte;
	private final CategoriaEdad categoria;
	private final int cupoMaximo;
	private boolean activo;
	private final List<MiembroEquipo> miembros;
	private final List<Suspension> suspensiones;


	public Equipo(String nombre, DeporteConjunto deporte, CategoriaEdad categoria, int cupoMaximo) {
		this.nombre = nombre;
		this.deporte = deporte;
		this.categoria = categoria;
		this.cupoMaximo = cupoMaximo;
		activo = true;
		miembros = new ArrayList<MiembroEquipo>();
		suspensiones = new ArrayList<Suspension>();
	}


	public void agregarJugador(Socio s, String posicion, int numero, LocalDate hoy) throws ReglaNegocioException {
		if (tieneJugador(s)) {
			throw new ReglaNegocioException(s.getNombre() + " ya es miembro de " + nombre);
		}

		if (!s.isActivo()) {
			throw new ReglaNegocioException(s.getNombre() + " no esta activo");
		}

		if (miembros.size() >= cupoMaximo) {
			throw new ReglaNegocioException(nombre + " ya tiene " + cupoMaximo + " jugadores, que es su cupo");
		}

		for (MiembroEquipo m : miembros) {
			if (m.getNumeroCamiseta() == numero) {
				throw new ReglaNegocioException("El dorsal " + numero + " ya esta asignado en " + nombre + "; elija un dorsal libre");
			}
		}

		int edadSocio = s.getEdad(hoy);
		if (!categoria.admite(edadSocio)) {
			throw new ReglaNegocioException(s.getNombre() + " tiene " + edadSocio + " anos y " + nombre + " es " + categoria.getNombre());
		}

		miembros.add(new MiembroEquipo(s, posicion, numero, hoy));
	}

	public boolean tieneJugador(Socio s) {
		for (MiembroEquipo m : miembros) {
			if (m.getSocio() == s) {
				return true;
			}
		}
		return false;
	}

	public boolean estaSuspendido(Socio s) {
		for (Suspension susp : suspensiones) {
			if (susp.getSocio() == s && !susp.isCumplida()) {
				return true;
			}
		}
		return false;
	}

	public boolean puedeAlinear(Socio s) {
		return tieneJugador(s) && s.estaHabilitado() && !estaSuspendido(s);
	}

	public int contarHabilitados() {
		int habilitados = 0;
		for (MiembroEquipo m : miembros) {
			if (puedeAlinear(m.getSocio())) {
				habilitados++;
			}
		}
		return habilitados;
	}

	public boolean puedeDisputarPartido() {
		return contarHabilitados() >= deporte.getMinJugadores();
	}

	public Map<Socio, String> jugadoresNoDisponibles() {
		Map<Socio, String> noDisponibles = new LinkedHashMap<Socio, String>();
		for (MiembroEquipo m : miembros) {
			String motivo = motivoNoDisponible(m.getSocio());
			if (motivo != null) {
				noDisponibles.put(m.getSocio(), motivo);
			}
		}
		return noDisponibles;
	}

	private String motivoNoDisponible(Socio s) {
		if (!s.isActivo()) {
			return "inactivo en el club";
		}
		for (Suspension susp : suspensiones) {
			if (susp.getSocio() == s && !susp.isCumplida()) {
				return "suspendido por " + susp.getMotivo();
			}
		}
		for (RestriccionMedica r : s.getRestricciones()) {
			if (r.estaVigente()) {
				return "restriccion medica: " + r.getMotivo();
			}
		}
		return null;
	}

	public void agregarSuspension(Suspension s) {
		suspensiones.add(s);
	}

	public void cumplirSuspensiones() {
		for (Suspension s : suspensiones) {
			if (!s.isCumplida()) {
				s.cumplir();
			}
		}
	}

	public void darDeBaja() {
		activo = false;
	}


	public String getNombre() {
		return nombre;
	}

	public DeporteConjunto getDeporte() {
		return deporte;
	}

	public CategoriaEdad getCategoria() {
		return categoria;
	}

	public int getCupoMaximo() {
		return cupoMaximo;
	}

	public boolean isActivo() {
		return activo;
	}

	public List<Suspension> getSuspensiones() {
		return suspensiones;
	}

	public List<MiembroEquipo> getMiembros() {
		return miembros;
	}

}
