package org.xmind.ui.internal.dialogs;

import java.util.List;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.xmind.core.ISheet;
import org.xmind.core.IWorkbook;
import org.xmind.gef.EditDomain;
import org.xmind.gef.GEF;
import org.xmind.gef.command.Command;
import org.xmind.gef.command.ICommandStack;
import org.xmind.gef.ui.editor.IGraphicalEditor;
import org.xmind.gef.util.Properties;
import org.xmind.ui.commands.NewSheetFromTemplateCommand;
import org.xmind.ui.gallery.GalleryLayout;
import org.xmind.ui.gallery.GalleryNavigablePolicy;
import org.xmind.ui.gallery.GallerySelectTool;
import org.xmind.ui.gallery.GalleryViewer;
import org.xmind.ui.internal.ITemplateDescriptor;
import org.xmind.ui.internal.MindMapMessages;
import org.xmind.ui.internal.MindMapTemplateManager;
import org.xmind.ui.internal.wizards.TemplateLabelProvider;

public class NewSheetFromTemplateDialog extends Dialog implements
        ISelectionChangedListener, IOpenListener {
    private static final int FRAME_WIDTH = 130;
    private static final int FRAME_HEIGHT = 90;

    private ITemplateDescriptor template;

    private IWorkbook currentWorkbook;

    private IGraphicalEditor editor;

    private Button chooseButton;

    private boolean chooseButtonVisible;

    public NewSheetFromTemplateDialog(IWorkbook currenIWorkbook,
            IGraphicalEditor editor) {
        this(null, currenIWorkbook, editor);
    }

    public NewSheetFromTemplateDialog(Shell parentShell,
            IWorkbook currentIWorkbook, IGraphicalEditor editor) {
        super(parentShell);
        this.currentWorkbook = currentIWorkbook;
        this.editor = editor;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(MindMapMessages.NewSheetFromTemplateDialog_text);
    }

    @Override
    protected Point getInitialSize() {
        return new Point(531, 540);
    }

    @Override
    protected void setShellStyle(int newShellStyle) {
        super.setShellStyle(SWT.MIN | SWT.MAX | SWT.CLOSE | SWT.RESIZE);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        applyDialogFont(composite);

        Control pageContent = createTemplatesContainer(composite);
        pageContent.setVisible(true);

        return composite;
    }

    private Control createTemplatesContainer(Composite parent) {
        GalleryViewer viewer = new GalleryViewer();

        EditDomain editDomain = new EditDomain();
        editDomain.installTool(GEF.TOOL_SELECT, new GallerySelectTool());
        editDomain.installEditPolicy(GalleryViewer.POLICY_NAVIGABLE,
                new GalleryNavigablePolicy());
        viewer.setEditDomain(editDomain);

        Properties properties = viewer.getProperties();
        properties.set(GalleryViewer.Wrap, true);
        properties.set(GalleryViewer.FlatFrames, true);
        properties.set(GalleryViewer.Horizontal, true);
        properties
                .set(GalleryViewer.TitlePlacement, GalleryViewer.TITLE_BOTTOM);
        properties.set(GalleryViewer.SolidFrames, true);
        properties.set(GalleryViewer.FrameContentSize, new Dimension(
                FRAME_WIDTH, FRAME_HEIGHT));
        properties.set(GalleryViewer.ImageConstrained, true);
        properties.set(GalleryViewer.Layout, new GalleryLayout(
                GalleryLayout.ALIGN_TOPLEFT, GalleryLayout.ALIGN_TOPLEFT, 10,
                10, 10, 10, 10, 10));
//        properties.set(GalleryViewer.Layout, new GridLayout());

        Control control = viewer.createControl(parent, SWT.BORDER);
        control.setLayoutData(new GridData(GridData.FILL_BOTH));
//        createTemplateDndSupport(control, viewer);
        viewer.setLabelProvider(new TemplateLabelProvider());
        List<ITemplateDescriptor> templates = loadTemplatesViewerInput();
        this.template = templates.get(0);
        viewer.setInput(templates);

        if (templates.size() > 0) {
            viewer.setSelection(new StructuredSelection(templates.get(0)), true);
        }
        viewer.addSelectionChangedListener(this);
        viewer.addOpenListener(this);

        return control;
    }

    private List<ITemplateDescriptor> loadTemplatesViewerInput() {
        return MindMapTemplateManager.getInstance().loadAllTemplates();
    }

    @Override
    protected void okPressed() {
        NewSheetFromTemplateCommand command = new NewSheetFromTemplateCommand(
                template, currentWorkbook);
        command.setLabel(MindMapMessages.NewSheetFromTemplateDialog_NewSheetFromTemplteCommand_label);
//        command.execute();
        saveAndRun(command);
        @SuppressWarnings("unchecked")
        List<ISheet> sheets = (List<ISheet>) command.getSource();
        renameCreatedSheets(sheets);
        super.okPressed();
    }

    protected ICommandStack getCommandStack() {
        return editor == null ? null : editor.getCommandStack();
    }

    protected void saveAndRun(Command command) {
        ICommandStack cs = getCommandStack();
        if (cs != null) {
            cs.execute(command);
        }
    }

    private void renameCreatedSheets(List<ISheet> sheets) {
        if (sheets != null && !sheets.isEmpty()) {
            for (ISheet sheet : sheets) {
                renameCreatedSheet(sheet);
            }
        }
    }

    private void renameCreatedSheet(ISheet sheet) {
        sheet.setTitleText(NLS.bind(MindMapMessages.TitleText_Sheet, sheet
                .getParent().getSheets().size()));
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.CANCEL_ID,
                IDialogConstants.CANCEL_LABEL, false);
        chooseButton = createButton(parent, IDialogConstants.OK_ID, MindMapMessages.NewSheetFromTemplateDialog_button_Choose,
                true);
    }

    public void open(OpenEvent event) {
//        Object selectedObject = event.getSource();
//        if (selectedObject instanceof ITemplateDescriptor) {
//            template = (ITemplateDescriptor) selectedObject;
//        }
        okPressed();

    }

    public void selectionChanged(SelectionChangedEvent event) {
//        Object selectedObject = event.getSource();
        ISelection selection = event.getSelection();
        if (selection instanceof StructuredSelection) {
            Object selectedObject = ((StructuredSelection) event.getSelection())
                    .getFirstElement();
            if (selectedObject instanceof ITemplateDescriptor) {
                template = (ITemplateDescriptor) selectedObject;
                chooseButtonVisible = true;
            } else {
                chooseButtonVisible = false;
            }
            chooseButton.setEnabled(chooseButtonVisible);
        }
    }

}
