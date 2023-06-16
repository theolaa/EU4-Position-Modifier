package EU4PositionModifier;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import pdx_script_parser.ParadoxScriptFile;
import pdx_script_parser.ParadoxScriptNode;

import com.formdev.flatlaf.FlatLightLaf;

public class EU4PositionModifier {

	private static enum Operation {
		SHIFT, SCALE
	}

	private static JFrame f = new JFrame();
	private static GridBagConstraints c = new GridBagConstraints();
	private static JTextArea status = new JTextArea();
	private static JCheckBox wrapOutOfBounds;
	private static JButton startButton;

	private static File inputFolder;
	private static File modFolder;
	private static File outputFolder;

	public static String modName;
	public static final String defaultOutputDirectory = new File(
			System.getProperty("user.home") + "/Desktop/PDXSP Output/").toString();

	public static void main(String[] args) {
		String gameName;
		if (args.length > 0 && "-egs".equals(args[0].toLowerCase())) {
			gameName = "Europa Universalis IV EGS";
		} else {
			gameName = "Europa Universalis IV";
		}
		inputFolder = new File(System.getProperty("user.home") + "/Documents/Paradox Interactive/" + gameName + "/mod");
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				FlatLightLaf.install();
				createAndShowGUI();
			}
		});

	}

	private static ComboBoxOption[] getModPaths() {
		ArrayList<ComboBoxOption> modPaths = new ArrayList<ComboBoxOption>();
		for (File f : inputFolder.listFiles()) {
			if (f.isDirectory() && new File(f, "/descriptor.mod").exists()) {
				String folder = f.getName();
				ParadoxScriptFile descriptor = new ParadoxScriptFile(new File(f, "/descriptor.mod"));
				String name;
				if (descriptor.ready()) {
					name = descriptor.getRootNode().getChildByIdentifier("name").getValue().replaceAll("\"", "");
				} else {
					name = folder;
				}

				ComboBoxOption option = new ComboBoxOption(name, folder);
				modPaths.add(option);
			}
		}
		ComboBoxOption[] result = new ComboBoxOption[modPaths.size()];
		result = modPaths.toArray(result);
		if (modPaths.isEmpty()) {
			updateStatus("EU4 Mod Directory Not Found");
			startButton.setEnabled(false);
		}
		return result;
	}

	static float modifyValue(float value, float offset, int upperBound, Operation operation) {
		switch (operation) {
		case SHIFT: {
			value += offset;
			if (value >= upperBound && upperBound > 0)
				value %= upperBound;
			return value;
		}
		case SCALE: {
			value *= offset;
			if (value >= upperBound && upperBound > 0)
				value %= upperBound;
			return value;
		}
		default:
			return value;
		}
	}

	static void saveFile(ParadoxScriptFile f, int saveMethod, File outputFolder) {
		switch (saveMethod) {
		case 0:
			f.saveDirect();
			break;
		case 1:
			f.saveDirectWithBackup();
			break;
		case 2:
			f.saveToSeparateFolder(outputFolder);
			break;
		default:
			updateStatus("Invalid save method selected: " + saveMethod);
		}
	}

	private static void createAndShowGUI() {

		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.getContentPane().setLayout(new BorderLayout());
		f.setTitle("EU4 Position Modifier");

		JPanel topbar = new JPanel(new GridBagLayout());

		status.setEditable(false);

		startButton = new JButton("<html><b>START<b></html>");

		// Select Mod Section
		JLabel selectModLabel = new JLabel("Select Mod: ");
		JComboBox<ComboBoxOption> selectMod = new JComboBox<ComboBoxOption>(getModPaths());
		status.setText("");
		ComboBoxOption selectedOption = (ComboBoxOption) selectMod.getSelectedItem();
		updateStatus("Selected mod: " + selectedOption.getDisplayName() + "\nFolder: mod/" + selectedOption.getValue());

		selectMod.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				status.setText("");
				ComboBoxOption selectedOption = (ComboBoxOption) selectMod.getSelectedItem();
				updateStatus("Selected mod: " + selectedOption.getDisplayName() + "\nFolder: mod/"
						+ selectedOption.getValue());
			}
		});

		// Output Directory Section
		JLabel selectOutputDirectoryLabel = new JLabel("Select Output Directory: ");
		JTextField selectOutputDirectory = new JTextField(defaultOutputDirectory);
		selectOutputDirectory.setEditable(false);
		JFileChooser selectOutputDirectoryNew = new JFileChooser(defaultOutputDirectory);
		selectOutputDirectoryNew.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		selectOutputDirectoryNew.setAcceptAllFileFilterUsed(false);

		JButton openOutputDirectory = new JButton("Open Output Directory");

		JButton openSelectDirectoryDialgogue = new JButton("Set Folder");
		openSelectDirectoryDialgogue.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				int statusResult = selectOutputDirectoryNew.showOpenDialog(f);
				if (statusResult == 0) {
					selectOutputDirectory.setText(selectOutputDirectoryNew.getSelectedFile().toString());
					openOutputDirectory.setEnabled(true);
				}
			}
		});

		openOutputDirectory.setEnabled(false);
		openOutputDirectory.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					Desktop.getDesktop().open(new File(selectOutputDirectory.getText()));
				} catch (IllegalArgumentException e) {
					updateStatus("Folder does not exist: " + selectOutputDirectory.getText());
					updateStatus("Create the folder first, or use the 'Set Folder' button.");
					updateStatus();
				} catch (IOException e) {
					updateStatus("You don't have permission to use: " + selectOutputDirectory.getText());
					updateStatus();
				}
			}
		});

		if (new File(selectOutputDirectory.getText()).exists()) {
			openOutputDirectory.setEnabled(true);
		}

		JLabel saveMethodLabel = new JLabel("Save Method: ");
		JComboBox<String> selectSaveMethod = new JComboBox<String>(
				new String[] { "Overwrite", "Overwrite with backup", "Separate output folder" });
		selectSaveMethod.setSelectedIndex(2);
		selectSaveMethod.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				// Save to output folder
				if (selectSaveMethod.getSelectedIndex() == 2) {
					selectOutputDirectory.setEnabled(true);
					openSelectDirectoryDialgogue.setEnabled(true);
				} else {
					selectOutputDirectory.setEnabled(false);
					openSelectDirectoryDialgogue.setEnabled(false);
				}
			}
		});

		JLabel filesToEditLabel = new JLabel("Files to edit: ");
		JPanel filesToEditxArea = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JCheckBox positionsCheckbox = new JCheckBox("Positions", true);
		JCheckBox tradeNodesCheckbox = new JCheckBox("Trade Nodes", true);
		JCheckBox ambientObjectsCheckbox = new JCheckBox("Ambient Objects", true);
		JCheckBox adjacenciesCheckbox = new JCheckBox("Adjacencies", false);
		JCheckBox lakesCheckbox = new JCheckBox("Lakes", false);

		JLabel offsetSettingsLabel = new JLabel("Adjust Positions By: ");
		JPanel offsetSettingsArea = new JPanel(new FlowLayout(FlowLayout.LEFT));
		SpinnerModel offsetSettingsXSpinnerModel = new SpinnerNumberModel(0, -100000, 100000, 0.01);
		SpinnerModel offsetSettingsYSpinnerModel = new SpinnerNumberModel(0, -100000, 100000, 0.01);
		SpinnerModel offsetSettingsZSpinnerModel = new SpinnerNumberModel(0, -100000, 100000, 0.01);
		JLabel offsetSettingsXLabel = new JLabel("X: ");
		JSpinner offsetSettingsXSpinner = new JSpinner(offsetSettingsXSpinnerModel);
		JLabel offsetSettingsYLabel = new JLabel("Y: ");
		JSpinner offsetSettingsYSpinner = new JSpinner(offsetSettingsYSpinnerModel);
		JLabel offsetSettingsZLabel = new JLabel("Z: ");
		JSpinner offsetSettingsZSpinner = new JSpinner(offsetSettingsZSpinnerModel);

		JLabel selectOperationLabel = new JLabel("Method: ");
		JComboBox<String> selectOperation = new JComboBox<String>(new String[] { "Shift", "Scale" });
		selectOperation.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if ("Shift".equals(selectOperation.getSelectedItem().toString())) {
					offsetSettingsXSpinner.setValue(0);
					offsetSettingsYSpinner.setValue(0);
					offsetSettingsZSpinner.setValue(0);

					wrapOutOfBounds.setEnabled(true);

				} else {
					offsetSettingsXSpinner.setValue(1);
					offsetSettingsYSpinner.setValue(1);
					offsetSettingsZSpinner.setValue(1);

					wrapOutOfBounds.setSelected(false);
					wrapOutOfBounds.setEnabled(false);

				}
			}
		});

		wrapOutOfBounds = new JCheckBox("Wrap out-of-bounds points");

		startButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				long startTime = System.currentTimeMillis();

				int saveMethod = selectSaveMethod.getSelectedIndex();

				modFolder = new File(inputFolder, ((ComboBoxOption) selectMod.getSelectedItem()).getValue());
				outputFolder = new File(selectOutputDirectory.getText());
				emptyFolder(outputFolder);
				if (saveMethod == 2) {
					outputFolder.mkdir();
					openOutputDirectory.setEnabled(true);
				}

				Operation operation;
				if ("Shift".equals(selectOperation.getSelectedItem().toString())) {
					operation = Operation.SHIFT;
				} else {
					operation = Operation.SCALE;
				}

				Float xOffset = Float.parseFloat(offsetSettingsXSpinner.getValue().toString());
				Float yOffset = Float.parseFloat(offsetSettingsYSpinner.getValue().toString());
				Float zOffset = Float.parseFloat(offsetSettingsZSpinner.getValue().toString());

				int maxMapX = 0;
				int maxMapZ = 0;
				// Process map dimensions
				if (wrapOutOfBounds.isSelected()) {
					if (!new File(modFolder, "map/default.map").exists()) {
						updateStatus(
								"default.map not found: File must be present to enable \"Wrap out-of-bounds points\"\nThis operation has been cancelled.");
						return;
					} else {
						ParadoxScriptFile defaultMapFile = new ParadoxScriptFile(
								new File(modFolder, "map/default.map"));
						maxMapX = Integer
								.parseInt(defaultMapFile.getRootNode().getChildByIdentifier("width").getValue());
						maxMapZ = Integer
								.parseInt(defaultMapFile.getRootNode().getChildByIdentifier("height").getValue());
					}
				}

				// Process Positions
				if (positionsCheckbox.isSelected()) {
					if (new File(modFolder, "map/positions.txt").exists()) {
						if (saveMethod == 2) {
							new File(outputFolder, "map").mkdir();
						}
						ParadoxScriptFile positionsFile = new ParadoxScriptFile(
								new File(modFolder, "map/positions.txt"));
						for (ParadoxScriptNode province : positionsFile.getRootNode().getChildren()) {
							ParadoxScriptNode positions = province.getChildByIdentifier("position");
							if (positions.getChildren().size() != 14) {
								updateStatus("ERROR: Malformed positions entry for " + province.getIdentifier());
							} else {
								for (int i = 0; i < positions.getChildren().size(); i++) {
									ParadoxScriptNode valueNode = positions.getChildren().get(i);
									float value = Float.parseFloat(valueNode.getIdentifier());
									if (i % 2 == 0) {
										value = modifyValue(value, xOffset, maxMapX, operation);
									} else {
										value = modifyValue(value, zOffset, maxMapZ, operation);
									}
									valueNode.setIdentifier(Float.toString(value));
								}
							}
						}
						saveFile(positionsFile, saveMethod, new File(outputFolder, "map"));
					} else {
						updateStatus("No file found at map/positions.txt");
					}
				}

				// Process Trade Nodes
				if (tradeNodesCheckbox.isSelected()) {
					if (new File(modFolder, "common/tradenodes").exists()) {
						if (saveMethod == 2) {
							new File(outputFolder, "common/tradenodes").mkdirs();
						}
						for (File f : new File(modFolder, "common/tradenodes").listFiles()) {
							ParadoxScriptFile tradeNodesFile = new ParadoxScriptFile(f);
							for (ParadoxScriptNode node : tradeNodesFile.getRootNode().getChildren()) {
								for (ParadoxScriptNode outgoing : node.getChildrenByIdentifier("outgoing")) {
									ParadoxScriptNode control = outgoing.getChildByIdentifier("control");
									for (int i = 0; i < control.getChildren().size(); i++) {
										ParadoxScriptNode valueNode = control.getChildren().get(i);
										float value = Float.parseFloat(valueNode.getIdentifier());
										if (i % 2 == 0) {
											value = modifyValue(value, xOffset, maxMapX, operation);
										} else {
											value = modifyValue(value, zOffset, maxMapZ, operation);
										}
										valueNode.setIdentifier(Float.toString(value));
									}
								}
							}
							saveFile(tradeNodesFile, saveMethod, new File(outputFolder, "common/tradenodes"));
						}
					} else {
						updateStatus("No files found at common/tradenodes");
					}
				}

				// Process Ambient Objects
				if (ambientObjectsCheckbox.isSelected()) {
					if (new File(modFolder, "map/ambient_object.txt").exists()) {
						if (saveMethod == 2) {
							new File(outputFolder, "map").mkdir();
						}
						ParadoxScriptFile ambientObjectsFile = new ParadoxScriptFile(
								new File(modFolder, "map/ambient_object.txt"));
						for (ParadoxScriptNode node : ambientObjectsFile.getRootNode().getChildren()) {

						}
						saveFile(ambientObjectsFile, saveMethod, new File(outputFolder, "map"));
					} else {
						updateStatus("No file found at map/ambient_object.txt");
					}
				}

				// Process Adjacencies
				if (adjacenciesCheckbox.isSelected()) {
					if (new File(modFolder, "map/adjacencies.csv").exists()) {
						if (saveMethod == 2) {
							new File(outputFolder, "map").mkdir();
						}
						// TODO
					} else {
						updateStatus("No file found at map/adjacencies.csv");
					}
				}

				// Process Lakes
				if (lakesCheckbox.isSelected()) {
					if (new File(modFolder, "map/lakes").exists()) {
						if (saveMethod == 2) {
							new File(outputFolder, "map/lakes").mkdirs();
						}
						for (File f : new File(modFolder, "map/lakes").listFiles()) {
							ParadoxScriptFile lakesFile = new ParadoxScriptFile(f);
							for (ParadoxScriptNode node : lakesFile.getRootNode().getChildren()) {

							}
							saveFile(lakesFile, saveMethod, new File(outputFolder, "map/lakes"));
						}
					} else {
						updateStatus("No files found at map/lakes");
					}
				}
				updateStatus("\nCompleted in " + (System.currentTimeMillis() - startTime) + "ms");
				updateStatus("\n============================================================");
			}
		});

		JScrollPane scrollPane = new JScrollPane(status);

		JPanel infoArea = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel infoLabel = new JLabel("A tool by theolaa");
		JButton infoButton = new JButton("View Instructions");
		infoButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					Desktop.getDesktop().browse(new URL("https://github.com/theolaa/EU4-Position-Modifier/blob/master/README.md").toURI());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		c.insets = new Insets(5, 15, 5, 15);
		c.weighty = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.LINE_START;

		// Add Select Mod Section
		c.weightx = 0;
		c.gridx = 0;
		c.gridy = 0;
		topbar.add(selectModLabel, c);

		c.weightx = 1;
		c.gridx = 1;
		c.gridy = 0;
		topbar.add(selectMod, c);

		// Save method
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 0;
		topbar.add(saveMethodLabel, c);
		c.gridx = 1;
		c.gridwidth = 1;
		topbar.add(selectSaveMethod, c);

		// Add Select Output Directory Section
		c.weightx = 0;
		c.gridx = 0;
		c.gridy = 2;
		topbar.add(selectOutputDirectoryLabel, c);

		c.weightx = 1;
		c.gridx = 1;
		c.gridy = 2;
		topbar.add(selectOutputDirectory, c);

		c.weightx = 1;
		c.gridx = 2;
		c.gridy = 2;
		topbar.add(openSelectDirectoryDialgogue, c);

		// Files to Edit
		c.weightx = 0;
		c.gridx = 0;
		c.gridy = 3;
		topbar.add(filesToEditLabel, c);

		// Add Checkboxes
		filesToEditxArea.add(positionsCheckbox);
		filesToEditxArea.add(tradeNodesCheckbox);
		filesToEditxArea.add(ambientObjectsCheckbox);
//		filesToEditxArea.add(adjacenciesCheckbox);
//		filesToEditxArea.add(lakesCheckbox);
		c.gridx = 1;
		c.gridy = 3;
		c.weightx = 1;
		topbar.add(filesToEditxArea, c);

		c.weightx = 0;
		c.gridx = 2;
		c.gridy = 3;
		topbar.add(openOutputDirectory, c);

		// Offset Settings
		c.weightx = 0;
		c.gridx = 0;
		c.gridy = 4;
		topbar.add(offsetSettingsLabel, c);

		// Labels and Spinners
		offsetSettingsArea.add(offsetSettingsXLabel);
		offsetSettingsArea.add(offsetSettingsXSpinner);
		offsetSettingsArea.add(offsetSettingsYLabel);
		offsetSettingsArea.add(offsetSettingsYSpinner);
		offsetSettingsArea.add(offsetSettingsZLabel);
		offsetSettingsArea.add(offsetSettingsZSpinner);
		offsetSettingsArea.add(selectOperationLabel);
		offsetSettingsArea.add(selectOperation);
		offsetSettingsArea.add(wrapOutOfBounds);
		c.gridx = 1;
		c.gridy = 4;
		c.weightx = 1;
		topbar.add(offsetSettingsArea, c);

//		// Instructions
//		c.gridx = 0;
//		c.gridy = 7;
//		c.gridwidth = 3;
//		topbar.add(instructions, c);

		// Add Start Button
		c.gridx = 0;
		c.gridy = 5;
		c.weightx = 1;
		c.gridwidth = 3;
		topbar.add(startButton, c);

		// Controls
		f.add(topbar, BorderLayout.PAGE_START);

		// Status Bar
		f.add(scrollPane, BorderLayout.CENTER);

		infoArea.add(infoLabel);
		infoArea.add(infoButton);
		f.add(infoArea, BorderLayout.PAGE_END);

		f.setPreferredSize(new Dimension(1150, 600));
		f.setMinimumSize(new Dimension(1150, 450));
		f.pack();
		f.setLocationRelativeTo(null);
		f.setIconImage(new ImageIcon(EU4PositionModifier.class.getClassLoader().getResource("icon.png")).getImage());
		f.setVisible(true);
	}

	private static void emptyFolder(File folder) {
		File[] files = folder.listFiles();
		if (files != null) { // some JVMs return null for empty dirs
			for (File f : files) {
				if (f.isDirectory()) {
					emptyFolder(f);
				} else {
					f.delete();
				}
			}
			folder.delete();
		}
	}

	private static void updateStatus() {
		System.out.println();
		status.append("\n");
	}

	private static void updateStatus(String message) {
		System.out.println(message);
		status.append(message + "\n");
	}

}