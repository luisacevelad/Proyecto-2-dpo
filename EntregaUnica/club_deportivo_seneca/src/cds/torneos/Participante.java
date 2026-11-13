package cds.torneos;

import java.util.List;

import cds.usuarios.Usuario;

public interface Participante {

	String getNombre();

	FormatoTorneo getFormato();

	List<Usuario> getIntegrantes();

	boolean incluye(Usuario u);
}
