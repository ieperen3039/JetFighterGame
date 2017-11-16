// @author Geert van Ieperen

#version 330

layout (location = 0) in vec3 position;
layout (location = 1) in vec3 vertexNormal;

// normal of the vertex
out vec3 mvVertexNormal;
// position of the vertex
out vec3 mvVertexPosition;

uniform mat4 modelViewMatrix;
uniform mat4 projectionMatrix;


void main() {

	vec4 mvPosition = modelViewMatrix * vec4(position, 1.0);
    gl_Position = projectionMatrix * mvPosition;

	mvVertexNormal = normalize(modelViewMatrix * vec4(vertexNormal, 0.0)).xyz;
    mvVertexPosition = mvPosition.xyz;
}