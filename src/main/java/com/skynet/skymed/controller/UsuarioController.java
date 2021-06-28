package com.skynet.skymed.controller;

import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skynet.skymed.model.Usuario;
import com.skynet.skymed.repository.PessoaRepository;
import com.skynet.skymed.repository.UsuarioRepository;
import com.skynet.skymed.service.EmailService;
import com.skynet.skymed.util.GeradorDeSenha;
import com.skynet.skymed.util.GeradorDeToken;

@RestController
@RequestMapping("/usuario")
public class UsuarioController {
	

	@Autowired
	private UsuarioRepository usuarioDB;
	
	@Autowired
	private PessoaRepository pessoaDB;
	
	private GeradorDeToken getToken = new GeradorDeToken();
	
	private EmailService servicoDeEmailPaciente = new EmailService();
  
	@ExceptionHandler({ NestedRuntimeException.class })
    public ResponseEntity<Object> handleException(NestedRuntimeException ex) {
		return ResponseEntity.badRequest().body(ex.getMostSpecificCause().getMessage());
    }
	
	@PostMapping(path = "obtemUsuario")	
	public ResponseEntity<Object> login(@RequestBody Usuario object) {	
		var usuario = usuarioDB.findByEmail(object.getEmail());	
		var mensagemErro = "E-mail ou senha incorreta.";	

		if (usuario == null) {	
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mensagemErro);	
		}	

		if (!GeradorDeSenha.verificaSenha(object.getSenha(), usuario.getSenha(), usuario.getEmail())) {	
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mensagemErro);	
		}	

		usuario.setSenha("");	

		return ResponseEntity.ok(usuario);	
	}
	
	@PutMapping(path = "recuperarsenha")
	@PreAuthorize("hasRole('USUARIO')")
	public ResponseEntity<Object> recuperarSenha(@RequestBody Usuario object){
		var usuario = usuarioDB.findByEmail(object.getEmail());
		if (usuario == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuário não existe. Tenta denovo");
		}
		usuario.setTokenRedefinicaoSenha(getToken.geraToken());
		var paciente = pessoaDB.findByUsuarioId(object.getId());
		
		try {
			servicoDeEmailPaciente.enviaEmailRecuperarSenha(paciente.getNome(),usuario.getEmail(),usuario.getTokenRedefinicaoSenha());
			usuarioDB.save(usuario);
			return ResponseEntity.ok(usuario);
		} catch (Exception e) {
			
			
			e.printStackTrace();
		}
		
		return null;
	}
	
	@PutMapping(path = "alterarsenha")
	@PreAuthorize("hasRole('USUARIO')")
	public ResponseEntity<Object> alterarSenha(@RequestBody Usuario object){
		var usuario = usuarioDB.findByEmail(object.getEmail());
		if (usuario == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuário não existe. Tenta denovo");
		}
		usuario.setSenha(GeradorDeSenha.geraSenhaSegura(object.getSenha(), usuario.getEmail()));
		usuarioDB.save(usuario);
		return ResponseEntity.ok(usuario);
		
	 
	}

	@PutMapping(path = "trocarSenha")
	@PreAuthorize("hasRole('USUARIO')")
	public ResponseEntity<Object> trocarSenha(@RequestBody Usuario object) {
		var usuario = usuarioDB.findByEmail(object.getEmail());
		var mensagemErro = "Senha incorreta.";
		
		
		
		if (usuario == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuário não existe. Re-efetue login.");
		}

		if (!GeradorDeSenha.verificaSenha(object.getSenha(), usuario.getSenha(), object.getEmail())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mensagemErro);
		}

		usuario.setSenha(GeradorDeSenha.geraSenhaSegura(object.getNovaSenha(), object.getEmail()));

		usuarioDB.save(usuario);

		usuario.setSenha("");

		return ResponseEntity.ok(usuario);
	}

	@PostMapping(path = "logout")
	@PreAuthorize("hasRole('USUARIO')")
	public ResponseEntity<Object> logout(@RequestBody Usuario object) {
		var usuario = usuarioDB.findByEmail(object.getEmail());
		var mensagemErro = "E-mail ou senha incorreta.";

		if (usuario == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mensagemErro);
		}

		if (!usuario.getSenha().equals(object.getSenha())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mensagemErro);
		}

		usuario.setTokenAutenticacao("");

		usuarioDB.save(usuario);

		usuario.setSenha("");

		return ResponseEntity.ok(usuario);
	}

	@PutMapping(path = "autenticaConta")
	public ResponseEntity<Object> autenticaConta(@RequestBody Usuario object) {

		var usuario = usuarioDB.findByEmail(object.getEmail());

		if (usuario == null) {

			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuário não encontrado");

		}
		if (object.getTokenAutenticacaoEmail().equals(usuario.getTokenAutenticacaoEmail())) {

			usuario.setEhAutenticado(true);

			usuarioDB.save(usuario);

			usuario.setSenha("");

			return ResponseEntity.ok(usuario);

		} else {

			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Token Inválido");
		}

	}

	@GetMapping(path = "/{email}")
	public ResponseEntity<Object> getByEmail(@PathVariable("email") String email) {
		try {
			var usuario = usuarioDB.findByEmail(email);

			usuario.setSenha("");

			return ResponseEntity.ok(usuario);

		} catch (NoSuchElementException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Erro ao recuperar usuário");
		}
	}

}
