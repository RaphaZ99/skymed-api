package com.skynet.skymed.controller;

import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skynet.skymed.repository.HorarioRepository;
import com.skynet.skymed.model.Horario;

@RestController

@RequestMapping("/horario")
public class HorarioController {
	
	final String HORARIO_INEXISTENTE = "HORARIO_INEXISTENTE";
	@Autowired
	private HorarioRepository horarioDB;
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Object> deleteHorario(@PathVariable("id") Integer id) {
		var horario = getById(id);	
		
		if(horario.getBody().equals(HORARIO_INEXISTENTE)) {
			return horario;
		}

		horarioDB.deleteById((long) id);

		return ResponseEntity.ok("Horario Excluido");
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<Object> getById(@PathVariable("id") Integer id) {
		try {
			var horario = horarioDB.findById((long) id);

			

			return ResponseEntity.ok(horario.get());
		} catch (NoSuchElementException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(HORARIO_INEXISTENTE);
		}
	}
	
	

}
