package EU4PositionModifier;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Locale;

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

import pdx_script_parser.ParadoxScriptNode;
import pdx_script_parser.file_types.ParadoxScriptFile;

import com.formdev.flatlaf.FlatLightLaf;

public class EU4PositionModifier {

	private static enum Operation {
		SHIFT, SCALE
	}
	
	private static enum SAVE_METHOD {
		DIRECT, DIRECT_WITH_BACKUP, SEPARATE_OUTPUT_FOLDER
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
	private static String fileEncoding = "UTF8";

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
					name = descriptor.getChildByIdentifier("name").getValue().replaceAll("\"", "");
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

	static void processXYPositions(ParadoxScriptNode positionBlock, float xOffset, int maxMapX, float yOffset,
			int maxMapY, Operation operation) {
		for (int i = 0; i < positionBlock.getChildren().size(); i++) {
			ParadoxScriptNode valueNode = positionBlock.getChildren().get(i);
			float value = Float.parseFloat(valueNode.getIdentifier());
			if (i % 2 == 0) {
				value = modifyValue(value, xOffset, maxMapX, operation);
			} else {
				value = modifyValue(value, yOffset, maxMapY, operation);
			}
			/*TODO Force '.' as decimal separator so some users don't get 1,000 instead of 1.000*/ 
			valueNode.setIdentifier(String.format(Locale.CANADA, "%.3f", value));
		}
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

	static void saveFile(ParadoxScriptFile f, SAVE_METHOD saveMethod, File outputFolder) {
		switch (saveMethod) {
		case DIRECT:
			f.saveDirect();
			break;
		case DIRECT_WITH_BACKUP:
			f.saveDirectWithBackup();
			break;
		case SEPARATE_OUTPUT_FOLDER:
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

		JPanel outputButtonArea = new JPanel();
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
		JCheckBox lakesCheckbox = new JCheckBox("Lakes", true);
		
		JLabel loadFilesAs = new JLabel("Load files as: ");
		JComboBox<String> selectFileLoadEncoding = new JComboBox<String>(
				new String[] { "UTF-8", "ANSI (Windows-1252)"});
		selectFileLoadEncoding.setSelectedIndex(0);
		selectFileLoadEncoding.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				// Save to output folder
				if (selectFileLoadEncoding.getSelectedIndex() == 0) {
					fileEncoding = "UTF8";
				} else {
					fileEncoding = "Cp1252";
				}
			}
		});

		JLabel offsetSettingsLabel = new JLabel("Adjust Positions By: ");
		JPanel offsetSettingsArea = new JPanel(new FlowLayout(FlowLayout.LEFT));
		SpinnerModel offsetSettingsXSpinnerModel = new SpinnerNumberModel(0, -100000, 100000, 0.01);
		SpinnerModel offsetSettingsYSpinnerModel = new SpinnerNumberModel(0, -100000, 100000, 0.01);
		SpinnerModel offsetSettingsHeightSpinnerModel = new SpinnerNumberModel(0, -100000, 100000, 0.01);
		JLabel offsetSettingsXLabel = new JLabel("X: ");
		JSpinner offsetSettingsXSpinner = new JSpinner(offsetSettingsXSpinnerModel);
		JLabel offsetSettingsYLabel = new JLabel("Y: ");
		JSpinner offsetSettingsYSpinner = new JSpinner(offsetSettingsYSpinnerModel);
		JLabel offsetSettingsHeightLabel = new JLabel("Height: ");
		JSpinner offsetSettingsHeightSpinner = new JSpinner(offsetSettingsHeightSpinnerModel);

		JLabel selectOperationLabel = new JLabel("Method: ");
		JComboBox<String> selectOperation = new JComboBox<String>(new String[] { "Shift", "Scale" });
		selectOperation.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if ("Shift".equals(selectOperation.getSelectedItem().toString())) {
					offsetSettingsXSpinner.setValue(0);
					offsetSettingsHeightSpinner.setValue(0);
					offsetSettingsYSpinner.setValue(0);

					wrapOutOfBounds.setEnabled(true);

				} else {
					offsetSettingsXSpinner.setValue(1);
					offsetSettingsHeightSpinner.setValue(1);
					offsetSettingsYSpinner.setValue(1);

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

				SAVE_METHOD saveMethod = SAVE_METHOD.values()[selectSaveMethod.getSelectedIndex()];

				modFolder = new File(inputFolder, ((ComboBoxOption) selectMod.getSelectedItem()).getValue());
				outputFolder = new File(selectOutputDirectory.getText());
				emptyFolder(outputFolder);
				if (saveMethod == SAVE_METHOD.SEPARATE_OUTPUT_FOLDER) {
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
				Float heightOffset = Float.parseFloat(offsetSettingsHeightSpinner.getValue().toString());
				Float yOffset = Float.parseFloat(offsetSettingsYSpinner.getValue().toString());

				int maxMapX = 0;
				int maxMapY = 0;
				// Process map dimensions
				if (wrapOutOfBounds.isSelected()) {
					if (!new File(modFolder, "map/default.map").exists()) {
						updateStatus(
								"default.map not found: File must be present to enable \"Wrap out-of-bounds points\"\nThis operation has been cancelled.");
						return;
					} else {
						ParadoxScriptFile defaultMapFile = new ParadoxScriptFile(
								new File(modFolder, "map/default.map"), fileEncoding);
						maxMapX = Integer
								.parseInt(defaultMapFile.getChildByIdentifier("width").getValue());
						maxMapY = Integer
								.parseInt(defaultMapFile.getChildByIdentifier("height").getValue());
					}
				}

				// Process Positions
				if (positionsCheckbox.isSelected()) {
					if (new File(modFolder, "map/positions.txt").exists()) {
						if (saveMethod == SAVE_METHOD.SEPARATE_OUTPUT_FOLDER) {
							new File(outputFolder, "map").mkdir();
						}
						ParadoxScriptFile positionsFile = new ParadoxScriptFile(
								new File(modFolder, "map/positions.txt"), fileEncoding);
						for (ParadoxScriptNode province : positionsFile.getChildren()) {
							ParadoxScriptNode positions = province.getChildByIdentifier("position");
							if (positions.getChildren().size() != 14) {
								updateStatus("ERROR: Malformed positions entry for " + province.getIdentifier());
							} else {
								processXYPositions(positions, xOffset, maxMapX, yOffset, maxMapY, operation);
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
						if (saveMethod == SAVE_METHOD.SEPARATE_OUTPUT_FOLDER) {
							new File(outputFolder, "common/tradenodes").mkdirs();
						}
						for (File f : new File(modFolder, "common/tradenodes").listFiles()) {
							ParadoxScriptFile tradeNodesFile = new ParadoxScriptFile(f, fileEncoding);
							for (ParadoxScriptNode node : tradeNodesFile.getChildren()) {
								for (ParadoxScriptNode outgoing : node.getChildrenByIdentifier("outgoing")) {
									ParadoxScriptNode control = outgoing.getChildByIdentifier("control");
									processXYPositions(control, xOffset, maxMapX, yOffset, maxMapY, operation);
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
						if (saveMethod == SAVE_METHOD.SEPARATE_OUTPUT_FOLDER) {
							new File(outputFolder, "map").mkdir();
						}
						ParadoxScriptFile ambientObjectsFile = new ParadoxScriptFile(
								new File(modFolder, "map/ambient_object.txt"), fileEncoding);
						for (ParadoxScriptNode node : ambientObjectsFile.getChildren()) {
							for (ParadoxScriptNode object : node.getChildrenByIdentifier("object")) {

								// Extract all the current position values
								ArrayList<ParadoxScriptNode> positions = object.getChildByIdentifier("position")
										.getChildrenObject();
								float xVal = Float.parseFloat(positions.get(0).getIdentifier());
								float heightVal = Float.parseFloat(positions.get(1).getIdentifier());
								float yVal = Float.parseFloat(positions.get(2).getIdentifier());

								// Modify them according to the configured options
								xVal = modifyValue(xVal, xOffset, maxMapX, operation);
								yVal = modifyValue(yVal, yOffset, maxMapY, operation);

								// Y has special considerations since it should never be less than zero, and
								// wrapping doesn't make any sense for Y
								switch (operation) {
								case SHIFT:
									heightVal += heightOffset;
									break;
								case SCALE:
									heightVal *= heightOffset;
									break;
								default:
									break;
								}
								if (heightVal < 0) {
									heightVal = 0;
								}

								// Apply the updated values
								positions.get(0).setIdentifier(Float.toString(xVal));
								positions.get(1).setIdentifier(Float.toString(heightVal));
								positions.get(2).setIdentifier(Float.toString(yVal));
							}
						}
						saveFile(ambientObjectsFile, saveMethod, new File(outputFolder, "map"));
					} else {
						updateStatus("No file found at map/ambient_object.txt");
					}
				}

				// Process Lakes
				if (lakesCheckbox.isSelected()) {
					if (new File(modFolder, "map/lakes").exists()) {
						if (saveMethod == SAVE_METHOD.SEPARATE_OUTPUT_FOLDER) {
							new File(outputFolder, "map/lakes").mkdirs();
						}
						for (File f : new File(modFolder, "map/lakes").listFiles()) {
							ParadoxScriptFile lakesFile = new ParadoxScriptFile(f, fileEncoding);
							for (ParadoxScriptNode lake : lakesFile.getChildrenByIdentifier("lake")) {
								ParadoxScriptNode triangle_strip = lake.getChildByIdentifier("triangle_strip");
								processXYPositions(triangle_strip, xOffset, maxMapX, yOffset, maxMapY, operation);
//								float height = Float.parseFloat(lake.getChildByIdentifier("height").getValue());
//								switch (operation) {
//								case SHIFT:
//									height += heightOffset;
//									break;
//								case SCALE:
//									height *= heightOffset;
//									break;
//								default:
//									break;
//								}
//								if (height < 0) {
//									height = 0;
//								}
//								lake.getChildByIdentifier("height").setValue(Float.toString(height));
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
					Desktop.getDesktop().browse(
							new URI("https://github.com/theolaa/EU4-Position-Modifier/blob/master/README.md"));
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

		outputButtonArea.setLayout(new GridLayout(0,2,10,0));
		outputButtonArea.add(openSelectDirectoryDialgogue);
		outputButtonArea.add(openOutputDirectory);
		c.weightx = 1;
		c.gridx = 1;
		c.gridy = 3;
		topbar.add(outputButtonArea, c);

		// Files to Edit
		c.weightx = 0;
		c.gridx = 0;
		c.gridy = 4;
		topbar.add(filesToEditLabel, c);

		// Add Checkboxes
		filesToEditxArea.add(positionsCheckbox);
		filesToEditxArea.add(tradeNodesCheckbox);
		filesToEditxArea.add(ambientObjectsCheckbox);
		filesToEditxArea.add(lakesCheckbox);
		c.gridx = 1;
		c.gridy = 4;
		c.weightx = 1;
		topbar.add(filesToEditxArea, c);
		
		// Load files as
		c.weightx = 0;
		c.gridx = 0;
		c.gridy = 5;
		topbar.add(loadFilesAs, c);
		
		c.gridx = 1;
		c.gridy = 5;
		c.weightx = 1;
		topbar.add(selectFileLoadEncoding, c);
		
		// Offset Settings
		c.weightx = 0;
		c.gridx = 0;
		c.gridy = 6;
		topbar.add(offsetSettingsLabel, c);

		// Labels and Spinners
		offsetSettingsArea.add(offsetSettingsXLabel);
		offsetSettingsArea.add(offsetSettingsXSpinner);
		offsetSettingsArea.add(offsetSettingsYLabel);
		offsetSettingsArea.add(offsetSettingsYSpinner);
		offsetSettingsArea.add(offsetSettingsHeightLabel);
		offsetSettingsArea.add(offsetSettingsHeightSpinner);
		offsetSettingsArea.add(selectOperationLabel);
		offsetSettingsArea.add(selectOperation);
		offsetSettingsArea.add(wrapOutOfBounds);
		c.gridx = 1;
		c.gridy = 6;
		c.weightx = 1;
		topbar.add(offsetSettingsArea, c);

		// Add Start Button
		c.gridx = 0;
		c.gridy = 7;
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

		f.setPreferredSize(new Dimension(975, 450));
		f.setMinimumSize(new Dimension(975, 450));
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