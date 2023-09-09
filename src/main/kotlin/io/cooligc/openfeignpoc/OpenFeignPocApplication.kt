package io.cooligc.openfeignpoc

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import feign.codec.Decoder
import feign.codec.Encoder
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.cbor.Jackson2CborDecoder
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.*
import org.springframework.web.reactive.config.EnableWebFlux
import java.lang.Exception
import java.util.*

@SpringBootApplication
@EnableWebFlux
@EnableFeignClients
class OpenFeignPocApplication

fun main(args: Array<String>) {
	runApplication<OpenFeignPocApplication>(*args)
}

@Configuration
class AppConfiguration(private val mapper : ObjectMapper){

	@Bean
	fun decoder(): Decoder {
		return JacksonDecoder(mapper)
	}

	@Bean
	fun encoder(): Encoder {
		return JacksonEncoder(mapper)
	}
}

class TodoDTO(var id: String, var date: Date, var description: String)

@Repository
class TodoRepository(){
	val repository = mutableMapOf<String, TodoDTO> ()

	init {
		for(i in 1..10){
			var id = UUID.randomUUID().toString()
			var todo = TodoDTO(id, Date(), "bootstrap data")
			this.repository.put(id, todo);
		}
		println("Repository initialized with value ${repository}")
	}


	fun addTodo(todo: TodoDTO){
		this.repository.put(todo.id, todo)
	}

	fun removeTodo(id: String){
		this.repository.remove(id)
	}

	fun findAll() : MutableCollection<TodoDTO> {
		return this.repository.values
	}

	fun findOne(id: String) : TodoDTO {
		return findAll().filter { it -> it.id.equals(id, true) }.last()
	}
}

@RestController
@RequestMapping("/api/todos")
class ToDoController(private val todoRepository : TodoRepository){

	@GetMapping
	fun findAll() : MutableCollection<TodoDTO>{
		return this.todoRepository.findAll()
	}

	@GetMapping("/{id}")
	fun findOne(@PathVariable("id") id: String) : TodoDTO {
		return this.todoRepository.findOne(id)
	}

	@PostMapping
	fun createOne(@RequestBody todo : TodoDTO) : TodoDTO {
		val id = UUID.randomUUID().toString()
		todo.id = id
		this.todoRepository.addTodo(todo)
		return this.todoRepository.findOne(id)
	}

	@DeleteMapping("/{id}")
	fun delete(@PathVariable("id") id: String)  {
		this.todoRepository.removeTodo(id)
	}

}


@FeignClient(value = "pinRepository", url = "http://postalpincode.in/api/pincode/")
interface PinRepository{
	@GetMapping("{pin}")
	fun pinDetails(@PathVariable("pin") pin : String) : JsonNode
}

@Service
class PinService(private val pinRepository: PinRepository){

	fun pinDetails(pin : String) : JsonNode {
		return this.pinRepository.pinDetails(pin)
	}
}

@RestController
@RequestMapping("/api/pins")
class PINController(private val pinService : PinService){

	@GetMapping("/{pin}")
	fun findAddress(@PathVariable("pin") pin : String) : JsonNode {
		return this.pinService.pinDetails(pin);
	}
}




class ExceptionTo(var description: String, var id: String, var eventTs : Date)

@RestControllerAdvice
class GlobalExceptionHandler{


	@ExceptionHandler
	fun handleException(e : Exception) : ResponseEntity<ExceptionTo>{

		val exception = e?.message?.let { ExceptionTo(it, UUID.randomUUID().toString(), Date()) }

		return ResponseEntity(exception, HttpStatus.BAD_REQUEST)
	}


}