package cds.torneos;

import java.util.ArrayList;
import java.util.List;

import cds.conjunto.Equipo;
import cds.conjunto.MiembroEquipo;
import cds.usuarios.Usuario;

public class EquipoTorneo implements Participante {
	private final Equipo equipo;

	public EquipoTorneo(Equipo equipo) {
		this.equipo = equipo;
	}

	@Override
	public String getNombre() {
		return equipo.getNombre();
	}

	@Override
	public FormatoTorneo getFormato() {
		return FormatoTorneo.CONJUNTO;
	}

	@Override
	public List<Usuario> getIntegrantes() {
		List<Usuario> integrantes = new ArrayList<Usuario>();
		for (MiembroEquipo m : equipo.getMiembros()) {
			integrantes.add(m.getSocio());
		}
		return integrantes;
	}

	public Equipo getEquipo() {
		return equipo;
	}

	@Override
	public boolean incluye(Usuario u) {
		return getIntegrantes().contains(u);
	}
}
