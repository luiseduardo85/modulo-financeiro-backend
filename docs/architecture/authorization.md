# Authorization

Autenticação e autorização são separadas.

Modelo:
Usuario -> UsuarioEmpresa -> UsuarioEmpresaPerfil -> Perfil -> PerfilPermissao -> Permissao.

Application valida acesso à empresa e permissão.
Domain valida estado e regras de negócio.

O mecanismo técnico definitivo de identidade/contexto será conectado ao serviço externo posteriormente.
