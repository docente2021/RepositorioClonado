package mcuca.zona;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.data.Binder;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.event.ShortcutAction;
import com.vaadin.server.Page;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import mcuca.cierre.CierreCajaRepository;
import mcuca.establecimiento.Establecimiento;
import mcuca.establecimiento.EstablecimientoRepository;
import mcuca.mesa.Mesa;
import mcuca.mesa.MesaRepository;
import mcuca.pedido.LineaPedidoRepository;
import mcuca.pedido.PedidoRepository;
import mcuca.pedido.PedidoService;
import mcuca.usuario.UsuarioRepository;

@SpringComponent
@UIScope
public class ZonaEditor extends VerticalLayout {

	private static final long serialVersionUID = 1L;

	private final ZonaRepository repoZona;
	private final MesaRepository repoMesa;
	private final PedidoRepository repoPedido;
	private final PedidoService pedidoService;
	private final EstablecimientoRepository repoEstablecimiento;
	private LineaPedidoRepository lps;
	private CierreCajaRepository cierresCaja;

	private Zona zona;

	/* Fields to edit properties in Zona entity */
	Label title = new Label("Nueva Zona");
	TextField nombre = new TextField("Nombre");
	TextField aforo = new TextField("Aforo");
	ComboBox<Establecimiento> select = new ComboBox<>("Establecimiento");

	/* Action buttons */
	Button guardar = new Button("Guardar");
	Button cancelar = new Button("Cancelar");
	Button borrar = new Button("Borrar");
	CssLayout acciones = new CssLayout(guardar, cancelar, borrar);

	Binder<Zona> binder = new Binder<>(Zona.class);

	@Autowired
	public ZonaEditor(
			ZonaRepository repoZona, EstablecimientoRepository repoEstablecimiento,
			MesaRepository repoMesa, PedidoRepository repoPedido, CierreCajaRepository cierresCaja,
			LineaPedidoRepository lps, UsuarioRepository u) {
		this.repoPedido = repoPedido;
		this.repoMesa = repoMesa;
		this.repoZona = repoZona;
		this.repoEstablecimiento = repoEstablecimiento;
		this.cierresCaja = cierresCaja;
		this.lps = lps;
		this.pedidoService = new PedidoService(this.repoPedido, this.cierresCaja, this.lps, u);
		
		select.setItems((Collection<Establecimiento>) repoEstablecimiento.findAll());
		select.setEmptySelectionAllowed(false);
		select.setRequiredIndicatorVisible(true);
	
		nombre.setMaxLength(32);
		addComponents(title, nombre, aforo, select, acciones);

		// bind using naming convention
		//binder.bindInstanceFields(this);
		binder.forField(nombre)
		.asRequired("No puede estar vac??o")
		.withValidator(new StringLengthValidator("Este campo debe ser una cadena entre 4 y 32 caracteres", 4, 32))
		.bind(Zona::getNombre, Zona::setNombre);
		
		
		binder.forField(aforo)
		  .withNullRepresentation("")
		  .asRequired("No puede estar vac??o")
		  .withConverter(
		    new StringToIntegerConverter("Por favor introduce un n??mero"))
		  .bind("aforo");
		
		binder.forField(select).bind("establecimiento");

		// Configure and style components
		setSpacing(true);
		acciones.setStyleName(ValoTheme.LAYOUT_COMPONENT_GROUP);
		guardar.setStyleName(ValoTheme.BUTTON_PRIMARY);
		guardar.setClickShortcut(ShortcutAction.KeyCode.ENTER);

		// wire action buttons to guardar, borrar and reset
		//guardar.addClickListener(this::salvar);
		guardar.addClickListener(e -> {
			if(binder.isValid())
				repoZona.save(zona);
			else
				mostrarNotificacion(new Notification("Algunos campos del formulario deben corregirse"));
		});
		
		borrar.addClickListener(e -> borrarZona());
		cancelar.addClickListener(e -> editarZona(zona));
		setVisible(false);
	}
	
	private void mostrarNotificacion(Notification notification) {
        notification.setDelayMsec(1500);
        notification.show(Page.getCurrent());
    }
	
	public void salvar(ClickEvent e) {
		binder.setBean(zona);
		zona.setEstablecimiento(select.getValue());
		repoZona.save(zona);// TODO Auto-generated method stub
	}
	
	private void borrarZona()
	{
		List<Mesa> mesas = repoMesa.findByZona(zona);
		for(Mesa mesa : mesas)
			repoMesa.delete(mesa);
		this.pedidoService.deletePedidosbyZona(zona);
		
		repoZona.delete(zona);
	}

	public interface ChangeHandler {
		void onChange();
	}

	public final void editarZona(Zona c) {
		if (c == null) {
			setVisible(false);
			return;
		}
		final boolean persisted = c.getId() != null;
		if (persisted) {
			// Find fresh entity for editing
			zona = repoZona.findOne(c.getId());
		}
		else {
			zona = c;
		}
		cancelar.setVisible(persisted);

		// Bind mcuca properties to similarly named fields
		// Could also use annotation or "manual binding" or programmatically
		// moving values from fields to entities before saving
		
		binder.setBean(zona);
		select.setSelectedItem(zona.getEstablecimiento());
		setVisible(true);

		// A hack to ensure the whole form is visible
		guardar.focus();
		// Select all text in nombre field automatically
		nombre.selectAll();
	}

	public void setChangeHandler(ChangeHandler h) {
		// ChangeHandler is notified when either guardar or borrar is clicked
		//guardar.addClickListener(e -> h.onChange());
		guardar.addClickListener(e -> {
			if(binder.isValid())
				h.onChange();
		});
		borrar.addClickListener(e -> h.onChange());
	}

	public EstablecimientoRepository getRepoEstablecimiento() {
		return repoEstablecimiento;
	}

}