package org.protege.editor.owl.client.admin.ui;

import edu.stanford.protege.metaproject.ConfigurationManager;
import edu.stanford.protege.metaproject.api.PasswordHasher;
import edu.stanford.protege.metaproject.api.PolicyFactory;
import edu.stanford.protege.metaproject.api.SaltedPasswordDigest;
import edu.stanford.protege.metaproject.api.User;
import edu.stanford.protege.metaproject.api.exception.IdAlreadyInUseException;
import org.protege.editor.core.ui.error.ErrorLogPanel;
import org.protege.editor.core.ui.util.AugmentedJTextField;
import org.protege.editor.core.ui.util.InputVerificationStatusChangedListener;
import org.protege.editor.core.ui.util.VerifiedInputEditor;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.client.ClientSession;
import org.protege.editor.owl.client.admin.exception.NonMatchingPasswordException;
import org.protege.editor.owl.client.api.Client;
import org.protege.editor.owl.client.api.exception.AuthorizationException;
import org.protege.editor.owl.client.api.exception.ClientRequestException;
import org.protege.editor.owl.client.diff.ui.GuiUtils;
import org.protege.editor.owl.ui.UIHelper;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Rafael Gonçalves <br>
 * Stanford Center for Biomedical Informatics Research
 */
public class UserDialogPanel extends JPanel implements VerifiedInputEditor {
    private static final long serialVersionUID = -3315863208276044071L;
    private static final int FIELD_WIDTH = 20;
    private OWLEditorKit editorKit;
    private AugmentedJTextField id, name, email;
    private JRadioButton externalAuth, defaultAuth, sameAuth;
    private JPasswordField password, passwordConf;
    private JLabel idLbl, nameLbl, emailLbl, passwordLbl, passwordConfLbl, authMethodLbl;
    private final JTextArea errorArea = new JTextArea(1, FIELD_WIDTH*2);
    private JSeparator separator = new JSeparator();
    private int rowIndex = 0;
    private JPanel holderPanel;
    private boolean currentlyValid = false, isEditing = false;
    private List<InputVerificationStatusChangedListener> listeners = new ArrayList<>();
    private User selectedUser;

    /**
     * Constructor
     */
    public UserDialogPanel(OWLEditorKit editorKit) {
        this(editorKit, false);
    }

    public UserDialogPanel(OWLEditorKit editorKit, boolean isEditing) {
        this.editorKit = checkNotNull(editorKit);
        this.isEditing = checkNotNull(isEditing);
        initInputFields();
        initUi();
    }

    private void initInputFields() {
        id = new AugmentedJTextField(FIELD_WIDTH, "User identifier used to login");
        name = new AugmentedJTextField(FIELD_WIDTH, "User name");
        email = new AugmentedJTextField(FIELD_WIDTH, "User email address");
        sameAuth = (isEditing ? new JRadioButton("Same") : null);
        defaultAuth = new JRadioButton("Default");
        externalAuth = new JRadioButton("External");
        password = new JPasswordField();
        passwordConf = new JPasswordField();
        password.setBorder(GuiUtils.MATTE_BORDER);
        passwordConf.setBorder(GuiUtils.MATTE_BORDER);

        idLbl = new JLabel("User Id:");
        nameLbl = new JLabel("Name:");
        emailLbl = new JLabel("Email Address:");
        authMethodLbl = new JLabel("Authentication Method:");
        passwordLbl = new JLabel("Password:");
        passwordConfLbl = new JLabel("Confirm Password:");
        holderPanel = new JPanel(new GridBagLayout());

        ButtonGroup group = new ButtonGroup();
        if(isEditing) {
            group.add(sameAuth);
            sameAuth.addActionListener(radioListener);
        }
        group.add(defaultAuth);
        group.add(externalAuth);
        defaultAuth.addActionListener(radioListener);
        externalAuth.addActionListener(radioListener);

        addListener(id);
        addListener(name);
        addListener(email);
        addListener(password);
        addListener(passwordConf);
    }

    private void initUi() {
        setLayout(new BorderLayout());
        add(holderPanel, BorderLayout.CENTER);
        Insets insets = new Insets(0, 2, 2, 2);
        holderPanel.add(idLbl, new GridBagConstraints(0, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, insets, 0, 0));
        holderPanel.add(id, new GridBagConstraints(1, rowIndex, 1, 1, 1.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        rowIndex++;
        holderPanel.add(nameLbl, new GridBagConstraints(0, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, insets, 0, 0));
        holderPanel.add(name, new GridBagConstraints(1, rowIndex, 1, 1, 1.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        rowIndex++;
        holderPanel.add(emailLbl, new GridBagConstraints(0, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, insets, 0, 0));
        holderPanel.add(email, new GridBagConstraints(1, rowIndex, 1, 1, 1.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
        rowIndex++;

        JPanel buttonPanel = new JPanel(new GridLayout(1, 0));
        if(isEditing) {
            buttonPanel.add(sameAuth);
        }
        buttonPanel.add(defaultAuth);
        buttonPanel.add(externalAuth);

        holderPanel.add(authMethodLbl, new GridBagConstraints(0, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, new Insets(4, 2, 2, 2), 0, 0));
        holderPanel.add(buttonPanel, new GridBagConstraints(1, rowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.NONE, insets, 0, 0));
        rowIndex++;
        initPasswordUi(true);
        defaultAuth.setSelected(true);
    }

    private void initPasswordUi(boolean defaultAuthMethod) {
        int lclRowIndex = rowIndex;
        if(defaultAuthMethod) {
            Insets insets = new Insets(0, 2, 2, 2);
            holderPanel.add(separator, new GridBagConstraints(0, lclRowIndex, 2, 1, 1.0, 0.0, GridBagConstraints.BASELINE, GridBagConstraints.HORIZONTAL, new Insets(5, 2, 5, 2), 0, 0));
            lclRowIndex++;
            holderPanel.add(passwordLbl, new GridBagConstraints(0, lclRowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, insets, 0, 0));
            holderPanel.add(password, new GridBagConstraints(1, lclRowIndex, 1, 1, 1.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
            lclRowIndex++;
            holderPanel.add(passwordConfLbl, new GridBagConstraints(0, lclRowIndex, 1, 1, 0.0, 0.0, GridBagConstraints.BASELINE_TRAILING, GridBagConstraints.NONE, insets, 0, 0));
            holderPanel.add(passwordConf, new GridBagConstraints(1, lclRowIndex, 1, 1, 1.0, 0.0, GridBagConstraints.BASELINE_LEADING, GridBagConstraints.HORIZONTAL, insets, 0, 0));
            lclRowIndex++;
        } else {
            holderPanel.remove(separator);
            holderPanel.remove(passwordLbl);
            holderPanel.remove(password);
            holderPanel.remove(passwordConfLbl);
            holderPanel.remove(passwordConf);
            revalidate();
            repaint();
        }
        errorArea.setBackground(null);
        errorArea.setBorder(null);
        errorArea.setEditable(false);
        errorArea.setWrapStyleWord(true);
        errorArea.setLineWrap(true);
        errorArea.setFont(errorArea.getFont().deriveFont(12.0f));
        errorArea.setForeground(Color.RED);
        holderPanel.add(errorArea, new GridBagConstraints(0, lclRowIndex, 2, 1, 1.0, 0.0, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE, new Insets(12, 2, 0, 2), 0, 0));
    }

    private ActionListener radioListener = e -> {
        if(defaultAuth.isSelected()) {
            initPasswordUi(true);
            handleValueChange();
        } else if(externalAuth.isSelected()) {
            initPasswordUi(false);
            handleValueChange();
        } else if(sameAuth.isSelected()) {
            initPasswordUi(false);
            handleValueChange();
        }
    };

    private void setIsEditing(User user) {
        selectedUser = checkNotNull(user);
        id.setText(user.getId().get());
        name.setText(user.getName().get());
        email.setText(user.getEmailAddress().get());
    }

    private void addListener(JTextField field) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                handleValueChange();
            }

            public void removeUpdate(DocumentEvent e) {
                handleValueChange();
            }

            public void changedUpdate(DocumentEvent e) {
                handleValueChange();
            }
        });
    }

    private void handleValueChange() {
        errorArea.setText("");
        try {
            setValid(checkInputs());
        } catch (NonMatchingPasswordException | IdAlreadyInUseException e) {
            setValid(false);
            Throwable cause = e.getCause();
            if(cause != null) {
                errorArea.setText(cause.getMessage());
            }
            else {
                errorArea.setText(e.getMessage());
            }
        }
    }

    private boolean checkInputs() throws NonMatchingPasswordException, IdAlreadyInUseException {
        boolean allValid = true;
        if(!isEditing && isUserIdInUse(id.getText())) {
            throw new IdAlreadyInUseException("User identifier '" + id.getText() + "' is already in use by another user");
        }
        if(name.getText().trim().isEmpty()) {
            allValid = false;
        }
        if(email.getText().trim().isEmpty()) {
            allValid = false;
        }
        if(defaultAuth.isSelected()) {
            if (!(password.getPassword().length > 0)) {
                allValid = false;
            }
            if (!(passwordConf.getPassword().length > 0)) {
                allValid = false;
            }
            // if both fields are non-empty and not-matching, throw exception
            if (!Arrays.equals(password.getPassword(), passwordConf.getPassword())
                    && password.getPassword().length > 0 && passwordConf.getPassword().length > 0) {
                throw new NonMatchingPasswordException("The given passwords do not match");
            }
        }
        return allValid;
    }

    private boolean isUserIdInUse(String id) {
        Client client = ClientSession.getInstance(editorKit).getActiveClient();
        try {
            for(User user : client.getAllUsers()) {
                if(user.getId().get().equals(id)) {
                    return true;
                }
            }
        } catch (AuthorizationException | ClientRequestException e) {
            /* do nothing */
        }
        return false;
    }

    private boolean isUsingDefaultAuthentication() {
        return defaultAuth.isSelected();
    }

    private User createUser() {
        PolicyFactory f = ConfigurationManager.getFactory();
        return f.getUser(f.getUserId(id.getText()), f.getName(name.getText()), f.getEmailAddress(email.getText()));
    }

    private SaltedPasswordDigest hashPassword() {
        PolicyFactory f = ConfigurationManager.getFactory();
        PasswordHasher hasher = f.getPasswordHasher();
        return hasher.hash(f.getPlainPassword(new String(password.getPassword())), f.getSaltGenerator().generate());
    }

    private void addUser(User user) {
        Client client = ClientSession.getInstance(editorKit).getActiveClient();
        try {
            client.createUser(user, Optional.empty());
        } catch (AuthorizationException | ClientRequestException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
    }

    private void addUser(User user, SaltedPasswordDigest passwordDigest) {
        Client client = ClientSession.getInstance(editorKit).getActiveClient();
        try {
            client.createUser(user, Optional.of(passwordDigest));
        } catch (AuthorizationException | ClientRequestException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
    }

    private void updateUser(User user) {
        Client client = ClientSession.getInstance(editorKit).getActiveClient();
        try {
            if(externalAuth.isSelected() || sameAuth.isSelected()) {
                client.updateUser(selectedUser.getId(), user, Optional.empty());
            } else {
                client.updateUser(selectedUser.getId(), user, Optional.of(hashPassword()));
            }
        } catch (AuthorizationException | ClientRequestException e) {
            ErrorLogPanel.showErrorDialog(e);
        }
    }

    public static Optional<User> showDialog(OWLEditorKit editorKit) {
        UserDialogPanel panel = new UserDialogPanel(editorKit);
        int response = new UIHelper(editorKit).showValidatingDialog("Add New User", panel, null);
        if(response == JOptionPane.OK_OPTION) {
            User user = panel.createUser();
            if(panel.isUsingDefaultAuthentication()) {
                panel.addUser(user, panel.hashPassword());
            } else {
                panel.addUser(user);
            }
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public static Optional<User> showDialog(OWLEditorKit editorKit, User selectedUser) {
        UserDialogPanel panel = new UserDialogPanel(editorKit, true);
        panel.setIsEditing(selectedUser);
        Optional<User> user = showDialog(editorKit, panel, "Edit User: " + selectedUser.getName().get());
        if(user.isPresent()) {
            panel.updateUser(user.get());
            return Optional.of(user.get());
        }
        return Optional.empty();
    }

    private static Optional<User> showDialog(OWLEditorKit editorKit, UserDialogPanel panel, String header) {
        int response = new UIHelper(editorKit).showValidatingDialog(header, panel, null);
        if (response == JOptionPane.OK_OPTION) {
            return Optional.of(panel.createUser());
        }
        return Optional.empty();
    }

    private void setValid(boolean valid) {
        currentlyValid = valid;
        for (InputVerificationStatusChangedListener l : listeners){
            l.verifiedStatusChanged(currentlyValid);
        }
    }

    @Override
    public void addStatusChangedListener(InputVerificationStatusChangedListener listener) {
        listeners.add(listener);
        listener.verifiedStatusChanged(currentlyValid);
    }

    @Override
    public void removeStatusChangedListener(InputVerificationStatusChangedListener listener) {
        listeners.remove(listener);
    }
}
