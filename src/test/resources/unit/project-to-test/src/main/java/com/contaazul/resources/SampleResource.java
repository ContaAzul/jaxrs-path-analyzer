/**
 *
 */
package com.contaazul.resources;

import javax.ws.rs.POST;
import javax.ws.rs.GET;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author cleiton.cardoso
 *
 */
@Path("sample")
public class SampleResource {

	@GET
	@Path("url1")
	public String teste1() {
		return null;
	}

	@POST
	@Path("url1")
	public void teste2() {
	}

	@GET
	@Path("url1")
	@Produces(MediaType.APPLICATION_JSON)
	public String teste3() {
		return null;
	}

	@DELETE
	@Path("url1")
	public void teste4() {
	}

}
