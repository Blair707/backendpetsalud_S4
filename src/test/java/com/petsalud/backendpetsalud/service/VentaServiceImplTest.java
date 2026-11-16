package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Producto;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.entity.Venta;
import com.petsalud.backendpetsalud.repository.ProductoRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import com.petsalud.backendpetsalud.repository.VentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VentaServiceImplTest {

    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private VentaServiceImpl ventaService;

    private Usuario usuario;
    private Producto shampoo;
    private Producto antipulgas;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L).email("juan.perez@example.com").password("secreto")
                .nombre("Juan").apellido("Perez").direccion("Calle Falsa 123")
                .rol(Rol.CLIENTE).build();
        shampoo = Producto.builder().id(10L).nombre("Shampoo canino")
                .precio(new BigDecimal("5990.50")).stock(5).build();
        antipulgas = Producto.builder().id(11L).nombre("Antipulgas")
                .precio(new BigDecimal("12000.00")).stock(2).build();
    }

    // ---------- Gestión de ventas: stock ----------

    @Test
    @DisplayName("Registra la venta cuando hay stock y descuenta una unidad por producto")
    void realizarVenta_conStock_registraVentaYDescuentaStock() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(shampoo, antipulgas));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));

        Venta venta = ventaService.realizarVenta(1L, List.of(10L, 11L));

        assertNotNull(venta);
        assertEquals(usuario, venta.getUsuario());
        assertEquals(2, venta.getProductos().size());
        assertNotNull(venta.getFecha());
        assertEquals(4, shampoo.getStock());
        assertEquals(1, antipulgas.getStock());
        verify(productoRepository, times(2)).save(any(Producto.class));
        verify(ventaRepository, times(1)).save(any(Venta.class));
    }

    @Test
    @DisplayName("No registra la venta si un producto no tiene stock")
    void realizarVenta_sinStock_lanzaExcepcionYNoGuarda() {
        shampoo.setStock(0);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findAllById(List.of(10L))).thenReturn(List.of(shampoo));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ventaService.realizarVenta(1L, List.of(10L)));

        assertTrue(ex.getMessage().contains("Shampoo canino"));
        assertTrue(ex.getMessage().contains("no tiene stock"));
        verify(ventaRepository, never()).save(any());
        verify(productoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lanza error si algún producto solicitado no existe")
    void realizarVenta_productoInexistente_lanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findAllById(List.of(10L, 99L))).thenReturn(List.of(shampoo));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ventaService.realizarVenta(1L, List.of(10L, 99L)));

        assertEquals("Algunos productos no fueron encontrados", ex.getMessage());
        verify(ventaRepository, never()).save(any());
    }

    // ---------- Gestión de ventas: total ----------

    @Test
    @DisplayName("El total de la venta es la suma de los precios de los productos")
    void realizarVenta_calculaTotalCorrecto() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(shampoo, antipulgas));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));

        Venta venta = ventaService.realizarVenta(1L, List.of(10L, 11L));

        assertEquals(new BigDecimal("17990.50"), venta.getTotal());
    }

    // ---------- Venta: usuario asociado ----------

    @Test
    @DisplayName("La venta queda asociada al usuario con datos completos")
    void realizarVenta_usuarioCompleto_ventaVinculadaAlUsuario() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findAllById(List.of(10L))).thenReturn(List.of(shampoo));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));

        ventaService.realizarVenta(1L, List.of(10L));

        ArgumentCaptor<Venta> captor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepository).save(captor.capture());
        Venta guardada = captor.getValue();
        assertSame(usuario, guardada.getUsuario());
        assertEquals("Juan", guardada.getUsuario().getNombre());
        assertEquals("Perez", guardada.getUsuario().getApellido());
        assertEquals("juan.perez@example.com", guardada.getUsuario().getEmail());
        assertEquals("Calle Falsa 123", guardada.getUsuario().getDireccion());
    }

    @Test
    @DisplayName("Lanza error si el usuario no existe")
    void realizarVenta_usuarioInexistente_lanzaExcepcion() {
        when(usuarioRepository.findById(5L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ventaService.realizarVenta(5L, List.of(10L)));

        assertEquals("Usuario no encontrado", ex.getMessage());
        verifyNoInteractions(productoRepository, ventaRepository);
    }

    @ParameterizedTest(name = "nombre = [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void realizarVenta_usuarioSinNombre_lanzaExcepcion(String nombre) {
        usuario.setNombre(nombre);
        assertVentaRechazadaPorDatosIncompletos();
    }

    @ParameterizedTest(name = "apellido = [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void realizarVenta_usuarioSinApellido_lanzaExcepcion(String apellido) {
        usuario.setApellido(apellido);
        assertVentaRechazadaPorDatosIncompletos();
    }

    @ParameterizedTest(name = "email = [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void realizarVenta_usuarioSinEmail_lanzaExcepcion(String email) {
        usuario.setEmail(email);
        assertVentaRechazadaPorDatosIncompletos();
    }

    @ParameterizedTest(name = "direccion = [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void realizarVenta_usuarioSinDireccion_lanzaExcepcion(String direccion) {
        usuario.setDireccion(direccion);
        assertVentaRechazadaPorDatosIncompletos();
    }

    private void assertVentaRechazadaPorDatosIncompletos() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ventaService.realizarVenta(1L, List.of(10L)));

        assertTrue(ex.getMessage().contains("datos incompletos"));
        verifyNoInteractions(productoRepository, ventaRepository);
    }

    // ---------- Permisos por rol ----------

    @ParameterizedTest
    @EnumSource(value = Rol.class, names = {"ADMIN", "CLIENTE"})
    @DisplayName("Los roles ADMIN y CLIENTE pueden realizar ventas")
    void realizarVenta_rolAutorizado_permiteVenta(Rol rol) {
        usuario.setRol(rol);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findAllById(List.of(10L))).thenReturn(List.of(shampoo));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));

        Venta venta = ventaService.realizarVenta(1L, List.of(10L));

        assertEquals(rol, venta.getUsuario().getRol());
    }

    @Test
    @DisplayName("El rol VETERINARIO no puede realizar ventas")
    void realizarVenta_rolNoAutorizado_lanzaExcepcion() {
        usuario.setRol(Rol.VETERINARIO);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ventaService.realizarVenta(1L, List.of(10L)));

        assertTrue(ex.getMessage().contains("no está autorizado"));
        verifyNoInteractions(productoRepository, ventaRepository);
    }

    @Test
    @DisplayName("Un usuario sin rol no puede realizar ventas")
    void realizarVenta_rolNulo_lanzaExcepcion() {
        usuario.setRol(null);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        assertThrows(IllegalArgumentException.class,
                () -> ventaService.realizarVenta(1L, List.of(10L)));
        verify(ventaRepository, never()).save(any());
    }

    // ---------- Relación venta - productos ----------

    @Test
    @DisplayName("Cada producto de la venta corresponde a los productos solicitados")
    void realizarVenta_productosVinculadosCorrectamente() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(shampoo, antipulgas));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));

        Venta venta = ventaService.realizarVenta(1L, List.of(10L, 11L));

        assertEquals(List.of(shampoo, antipulgas), venta.getProductos());
        assertTrue(venta.getProductos().stream().allMatch(p -> p.getId() == 10L || p.getId() == 11L));
    }

    // ---------- Consultas ----------

    @Test
    void obtenerVentaPorId_delegaEnRepositorio() {
        Venta venta = Venta.builder().id(7L).build();
        when(ventaRepository.findById(7L)).thenReturn(Optional.of(venta));

        assertEquals(Optional.of(venta), ventaService.obtenerVentaPorId(7L));
    }

    @Test
    void listarVentasPorUsuario_delegaEnRepositorio() {
        Venta venta = Venta.builder().id(7L).usuario(usuario).build();
        when(ventaRepository.findByUsuarioId(1L)).thenReturn(List.of(venta));

        assertEquals(List.of(venta), ventaService.listarVentasPorUsuario(1L));
    }

    @Test
    void listarTodasLasVentas_delegaEnRepositorio() {
        when(ventaRepository.findAll()).thenReturn(List.of(new Venta(), new Venta()));

        assertEquals(2, ventaService.listarTodasLasVentas().size());
    }
}
