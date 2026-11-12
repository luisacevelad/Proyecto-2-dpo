package cds.consola;

import cds.ReglaNegocioException;

/** Una accion de menu. Puede fallar por una regla de negocio. */
@FunctionalInterface
public interface Accion {
	void ejecutar() throws ReglaNegocioException;
}
