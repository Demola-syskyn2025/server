package com.deepen.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtUtil: JwtUtil
) : OncePerRequestFilter() {
    
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7)
            
            if (jwtUtil.validateToken(token)) {
                val email = jwtUtil.extractEmail(token)
                val role = jwtUtil.extractRole(token)
                
                if (email != null && role != null) {
                    val authorities = listOf(SimpleGrantedAuthority("ROLE_$role"))
                    val authentication = UsernamePasswordAuthenticationToken(email, null, authorities)
                    SecurityContextHolder.getContext().authentication = authentication
                }
            }
        }
        
        filterChain.doFilter(request, response)
    }
}
