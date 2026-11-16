package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L).email("juan.perez@example.com").password("secreto")
                .nombre("Juan").apellido("Perez").telefono("123456789")
                .direccion("Calle Falsa 123").rol(Rol.CLIENTE).build();
    }

    @Test
    @DisplayName("Registra un usuario con email nuevo")
    void registrarUsuario_emailNuevo_guardaUsuario() {
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.empty());
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        Usuario resultado = usuarioService.registrarUsuario(usuario);

        assertEquals(usuario, resultado);
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("No permite registrar un email duplicado")
    void registrarUsuario_emailDuplicado_lanzaExcepcion() {
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> usuarioService.registrarUsuario(usuario));

        assertEquals("El email ya está registrado", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void obtenerUsuarioPorId_existente_retornaUsuarioConDatosObligatorios() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        Usuario encontrado = usuarioService.obtenerUsuarioPorId(1L).orElseThrow();

        assertAll("datos obligatorios",
                () -> assertNotNull(encontrado.getNombre()),
                () -> assertNotNull(encontrado.getApellido()),
                () -> assertNotNull(encontrado.getEmail()),
                () -> assertNotNull(encontrado.getDireccion()));
    }

    @Test
    void obtenerUsuarioPorId_inexistente_retornaVacio() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertTrue(usuarioService.obtenerUsuarioPorId(99L).isEmpty());
    }

    @Test
    void obtenerUsuarioPorEmail_delegaEnRepositorio() {
        when(usuarioRepository.findByEmail("juan.perez@example.com")).thenReturn(Optional.of(usuario));

        assertEquals(Optional.of(usuario), usuarioService.obtenerUsuarioPorEmail("juan.perez@example.com"));
    }

    @Test
    @DisplayName("Actualiza nombre, apellido, teléfono y dirección, pero no email ni password")
    void actualizarUsuario_existente_actualizaSoloCamposPermitidos() {
        Usuario cambios = Usuario.builder().nombre("Pedro").apellido("Soto").telefono("987654321")
                .direccion("Av. Nueva 456").email("otro@example.com").password("otra").build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario actualizado = usuarioService.actualizarUsuario(1L, cambios);

        assertEquals("Pedro", actualizado.getNombre());
        assertEquals("Soto", actualizado.getApellido());
        assertEquals("987654321", actualizado.getTelefono());
        assertEquals("Av. Nueva 456", actualizado.getDireccion());
        assertEquals("juan.perez@example.com", actualizado.getEmail());
        assertEquals("secreto", actualizado.getPassword());
    }

    @Test
    void actualizarUsuario_inexistente_lanzaExcepcion() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> usuarioService.actualizarUsuario(99L, usuario));

        assertEquals("Usuario no encontrado", ex.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void eliminarUsuario_delegaEnRepositorio() {
        usuarioService.eliminarUsuario(1L);

        verify(usuarioRepository).deleteById(1L);
    }

    @Test
    void listarUsuarios_retornaTodos() {
        when(usuarioRepository.findAll()).thenReturn(List.of(usuario));

        assertEquals(1, usuarioService.listarUsuarios().size());
    }
}
