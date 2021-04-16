package br.com.avenue.receita.controllers;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.opencsv.CSVWriter;

import br.com.avenue.receita.services.ReceitaService;

@Controller
public class UploadController {

//	private final String UPLOAD_DIR = "src/main/resources/templates/uploads/";
//	private final String RETORNO_DIR = "src/main/resources/templates/retorno/";	
	private final String UPLOAD_DIR = "";
	private final String RETORNO_DIR = "";	
	

	@GetMapping("/")
	public String homepage() {
		return "index";
	}

	@PostMapping("/upload")
	public String uploadFile(@RequestParam("file") MultipartFile file, RedirectAttributes attributes) {

		// check if file is empty
		if (file.isEmpty()) {
			attributes.addFlashAttribute("message", "Por favor, selecione um arquivo CSV para upload.");
			return "redirect:/";
		}

		// normalize the file path
//        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
		String fileName = "arquivo.csv";

		// save the file on the local file system
		try {
			Path path = Paths.get(UPLOAD_DIR + fileName);
			Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}

		String arquivoCSV = UPLOAD_DIR + fileName;
		BufferedReader br = null;
		String linha = "";
		String csvDivisor = ";";

		try {
			br = new BufferedReader(new FileReader(arquivoCSV));
			ReceitaService receitaService = new ReceitaService();
			List<String[]> listSaida = new ArrayList<>();
			while ((linha = br.readLine()) != null) {

				String[] retorno = linha.split(csvDivisor);

				String agencia = retorno[0];
				String conta = retorno[1].replace("-", "");
				double saldo = Double.parseDouble(retorno[2].replace(",", "."));
				String status = retorno[3];

				boolean validarEnvio = receitaService.atualizarConta(agencia, conta, saldo, status);

				listSaida.add(
						new String[] { retorno[0], retorno[1], retorno[2], retorno[3], String.valueOf(validarEnvio) });
			}
			Writer writer = Files.newBufferedWriter(Paths.get(RETORNO_DIR + "arquivo_retorno.csv"));
			CSVWriter csvWriter = new CSVWriter(writer, ';', CSVWriter.NO_QUOTE_CHARACTER);
			csvWriter.writeAll(listSaida);
			csvWriter.flush();

			writer.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		attributes.addFlashAttribute("message", "Upload enviado com sucesso " + fileName + '!');
		attributes.addFlashAttribute("return", "ok");
		return "redirect:/";
	}

	@RequestMapping(value = "/download", method = RequestMethod.GET)
	public HttpEntity<byte[]> download() throws IOException {

		byte[] arquivo = Files.readAllBytes(Paths.get(RETORNO_DIR + "arquivo_retorno.csv"));

		HttpHeaders httpHeaders = new HttpHeaders();

		httpHeaders.add("Content-Disposition", "attachment;filename=\"arquivo_retorno.csv\"");

		HttpEntity<byte[]> entity = new HttpEntity<byte[]>(arquivo, httpHeaders);

		return entity;
	}

}
